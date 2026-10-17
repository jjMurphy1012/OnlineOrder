package com.laioffer.onlineorder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.laioffer.onlineorder.model.CartDto;
import com.laioffer.onlineorder.model.OrderItemDto;
import com.laioffer.onlineorder.model.RestaurantDto;
import com.laioffer.onlineorder.repository.CartRepository;
import com.laioffer.onlineorder.repository.MenuItemRepository;
import com.laioffer.onlineorder.repository.OrderItemRepository;
import com.laioffer.onlineorder.service.CartService;
import com.laioffer.onlineorder.service.CustomerService;
import com.laioffer.onlineorder.service.RestaurantService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.AopTestUtils;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"onlineorder.demo-data.enabled=false", "logging.level.root=WARN"})
@Import(JdbcMeasurementConfig.class)
@EnabledIfEnvironmentVariable(named = "ONLINEORDER_TEST_JDBC_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:.*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostgresIntegrationBenchmarkTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired CartService carts;
    @Autowired CustomerService customers;
    @Autowired RestaurantService restaurants;
    @Autowired CartRepository cartRepository;
    @Autowired MenuItemRepository menuRepository;
    @Autowired OrderItemRepository orderRepository;
    @Autowired CacheManager cacheManager;
    @Autowired ObjectMapper mapper;
    @LocalServerPort int port;
    private final List<Map<String,Object>> samples = new ArrayList<>();
    private final List<Map<String,Object>> groups = new ArrayList<>();
    private final JdbcMeasurementConfig.SqlCounter counter = JdbcMeasurementConfig.COUNTER;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) { PostgresTestDatabase.configure(registry); }

    @BeforeAll
    void syntheticFixtureOnly() throws Exception {
        assertEquals("onlineorder_benchmark", jdbc.queryForObject("SELECT current_database()",String.class));
        for (String statement : Files.readString(Path.of("benchmarks/fixtures.sql")).split(";")) {
            if (!statement.isBlank()) jdbc.execute(statement);
        }
        assertEquals(100L, jdbc.queryForObject("SELECT COUNT(*) FROM restaurants",Long.class));
        assertEquals(1000L, jdbc.queryForObject("SELECT COUNT(*) FROM menu_items",Long.class));
        assertEquals("org.springframework.cache.caffeine.CaffeineCacheManager",cacheManager.getClass().getName());
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    record Observation<T>(T value, double millis, List<String> sql) {}
    private <T> Observation<T> measure(Supplier<T> operation) {
        return measure(operation, true);
    }
    private <T> Observation<T> measure(Supplier<T> operation, boolean traceSql) {
        if (traceSql) counter.start();
        long start=System.nanoTime();
        T value;
        long elapsed;
        List<String> sql;
        try { value=operation.get(); elapsed=System.nanoTime()-start; }
        finally { sql=traceSql ? counter.stop() : List.of(); }
        return new Observation<>(value,elapsed/1_000_000.0,sql);
    }

    private void record(String group,String arm,int iteration,boolean warmup,int items,Observation<?> o) {
        Map<String,Object> row=new LinkedHashMap<>();
        row.put("group",group); row.put("arm",arm); row.put("iteration",iteration);
        row.put("warmup",warmup); row.put("line_items",items); row.put("elapsed_ms",o.millis());
        row.put("sql_count",group.endsWith("_timing_only") ? null : o.sql().size()); row.put("output_oracle_passed",true);
        row.put("sql_templates",JdbcMeasurementConfig.SqlCounter.histogram(o.sql()));
        samples.add(row);
    }

    private void assertCart(CartDto actual,int n,int base,long cartId) {
        List<OrderItemDto> expected=new ArrayList<>();
        double total=0;
        for(int i=1;i<=n;i++) {
            double price=2+(i%100)*0.25; int quantity=1+(i%3); total+=price*quantity;
            expected.add(new OrderItemDto((long)base+i,(long)i,(long)((i-1)/10+1),price,quantity,
                    "Menu "+i,"Synthetic menu item "+i,"fixture://menu/"+i));
        }
        assertEquals(cartId,actual.id()); assertEquals(total,actual.totalPrice());
        assertEquals(expected,actual.orderItems().stream().sorted(Comparator.comparing(OrderItemDto::orderItemId)).toList());
    }

    @Test @Order(1)
    void cartReadOldVersusBatchedUsesRealSqlAndCompleteDtoOracle() throws Exception {
        LegacyCartRead old=new LegacyCartRead(cartRepository,menuRepository,orderRepository);
        CartService current=AopTestUtils.getTargetObject(carts); // Both arms bypass cache advice.
        int[] sizes={1,10,50,100}; int[] bases={0,1,11,61};
        for(boolean traceSql : new boolean[]{true,false}) {
        for(int s=0;s<sizes.length;s++) {
            int n=sizes[s],base=bases[s]; long customerId=101+s,cartId=201+s;
            for(int i=-10;i<50;i++) {
                boolean oldFirst=Math.floorMod(i,2)==0;
                Observation<CartDto> a,b;
                if(oldFirst) { a=measure(()->old.getCart(customerId),traceSql); b=measure(()->current.getCart(customerId),traceSql); }
                else { b=measure(()->current.getCart(customerId),traceSql); a=measure(()->old.getCart(customerId),traceSql); }
                assertCart(a.value(),n,base,cartId); assertCart(b.value(),n,base,cartId);
                assertEquals(a.value(),b.value(),"old/current order and every DTO field must match");
                if (traceSql) { assertEquals(n+2,a.sql().size()); assertEquals(3,b.sql().size()); }
                record("cart_"+n+(traceSql ? "" : "_timing_only"),"old_n_plus_2",i,i<0,n,a);
                record("cart_"+n+(traceSql ? "" : "_timing_only"),"new_batch",i,i<0,n,b);
            }
        }
        }
        saveMeasurements();
    }

    private void assertMenu(List<RestaurantDto> menu) {
        assertEquals(100,menu.size());
        assertEquals(100L,menu.stream().map(RestaurantDto::id).distinct().count());
        assertEquals(1000L,menu.stream().flatMap(r->r.menuItems().stream()).map(m->m.id()).distinct().count());
        for(RestaurantDto restaurant:menu) {
            long id=restaurant.id(); assertEquals("Restaurant "+id,restaurant.name());
            assertEquals("Synthetic address "+id,restaurant.address());
            assertEquals("0000000000",restaurant.phone()); assertEquals("fixture://restaurant/"+id,restaurant.imageUrl());
            assertEquals(10,restaurant.menuItems().size());
            for(var item:restaurant.menuItems()) {
                long menuId=item.id(); assertEquals(id,(menuId-1)/10+1);
                assertEquals("Menu "+menuId,item.name()); assertEquals("Synthetic menu item "+menuId,item.description());
                assertEquals(1+(menuId%100)*0.25,item.price()); assertEquals("fixture://menu/"+menuId,item.imageUrl());
            }
        }
        assertEquals(1000,menu.stream().mapToInt(r->r.menuItems().size()).sum());
    }

    @Test @Order(2)
    void restaurantCatalogActualCaffeineMissAndHit() throws Exception {
        var cache=cacheManager.getCache("restaurants");
        for(boolean traceSql : new boolean[]{true,false}) {
        for(int i=-10;i<50;i++) {
            Observation<List<RestaurantDto>> miss,hit;
            if(Math.floorMod(i,2)==0) {
                cache.clear(); miss=measure(restaurants::getRestaurants,traceSql); hit=measure(restaurants::getRestaurants,traceSql);
            } else {
                cache.clear(); restaurants.getRestaurants(); // Setup/prior cache fill excluded.
                hit=measure(restaurants::getRestaurants,traceSql); cache.clear(); miss=measure(restaurants::getRestaurants,traceSql);
            }
            assertMenu(miss.value()); assertMenu(hit.value()); assertEquals(miss.value(),hit.value());
            if(traceSql) { assertEquals(2,miss.sql().size()); assertEquals(0,hit.sql().size()); }
            record("catalog_100_restaurants_1000_items"+(traceSql ? "" : "_timing_only"),"forced_miss",i,i<0,1000,miss);
            record("catalog_100_restaurants_1000_items"+(traceSql ? "" : "_timing_only"),"cache_hit",i,i<0,1000,hit);
        }
        }
        saveMeasurements();
    }

    @SuppressWarnings("unchecked")
    @Test @Order(3)
    void cacheHasConfiguredTtlAndExpiredEntryReloadsRealDatabase() throws Exception {
        var cache=cacheManager.getCache("restaurants");
        Cache<Object,Object> nativeCache=(Cache<Object,Object>)cache.getNativeCache();
        var expiration=nativeCache.policy().expireAfterWrite().orElseThrow();
        assertEquals(Duration.ofSeconds(60),expiration.getExpiresAfter());
        try {
            expiration.setExpiresAfter(Duration.ofMillis(15));
            cache.clear(); restaurants.getRestaurants();
            long deadline=System.nanoTime()+Duration.ofSeconds(2).toNanos();
            while(nativeCache.estimatedSize()>0 && System.nanoTime()<deadline) {
                Thread.sleep(5); nativeCache.cleanUp();
            }
            var reload=measure(restaurants::getRestaurants);
            assertEquals(2,reload.sql().size()); assertMenu(reload.value());
        } finally { expiration.setExpiresAfter(Duration.ofSeconds(60)); cache.clear(); }
    }

    private long newCustomer(String label) {
        String email=label+"@example.test";
        customers.signUp(email,"Synthetic-Pass-123!","Synthetic","Customer");
        return customers.getCustomerByEmail(email).id();
    }

    @Test @Order(4)
    void addAndClearInvalidateOnlyTheAffectedCustomerCache() {
        long a=newCustomer("cache-a"),b=newCustomer("cache-b");
        carts.getCart(a); carts.getCart(b);
        assertEquals(0,measure(()->carts.getCart(a)).sql().size());
        carts.addMenuItemToCart(a,1); carts.addMenuItemToCart(a,1);
        var afterAdd=measure(()->carts.getCart(a));
        assertEquals(3,afterAdd.sql().size()); assertEquals(1,afterAdd.value().orderItems().size());
        assertEquals(2,afterAdd.value().orderItems().getFirst().quantity()); assertEquals(2.5,afterAdd.value().totalPrice());
        assertEquals(0,measure(()->carts.getCart(b)).sql().size());
        carts.clearCart(a); var afterClear=measure(()->carts.getCart(a));
        assertEquals(2,afterClear.sql().size()); assertEquals(0.0,afterClear.value().totalPrice());
        assertTrue(afterClear.value().orderItems().isEmpty());
    }

    private void installRejectCartUpdate(long customerId) {
        jdbc.execute("CREATE OR REPLACE FUNCTION benchmark_reject_update() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.customer_id="+customerId+" THEN RAISE EXCEPTION 'synthetic injected failure'; END IF; RETURN NEW; END $$");
        jdbc.execute("CREATE TRIGGER benchmark_reject_cart_update BEFORE UPDATE ON carts FOR EACH ROW EXECUTE FUNCTION benchmark_reject_update()");
    }
    private void removeRejectCartUpdate() {
        jdbc.execute("DROP TRIGGER IF EXISTS benchmark_reject_cart_update ON carts");
        jdbc.execute("DROP FUNCTION IF EXISTS benchmark_reject_update()");
    }

    @Test @Order(5)
    void databaseFaultAfterLineWriteRollsBackAddAndPreservesCachedState() {
        long id=newCustomer("rollback-add"); CartDto before=carts.getCart(id);
        installRejectCartUpdate(id);
        counter.start();
        RuntimeException failure;
        List<String> statements;
        try { failure=assertThrows(RuntimeException.class,()->carts.addMenuItemToCart(id,1)); }
        finally { statements=counter.stop(); removeRejectCartUpdate(); }
        assertInjectedFailure(failure,statements,"insert");
        assertEquals(0L,jdbc.queryForObject("SELECT COUNT(*) FROM order_items oi JOIN carts c ON c.id=oi.cart_id WHERE c.customer_id=?",Long.class,id));
        assertEquals(0.0,jdbc.queryForObject("SELECT total_price FROM carts WHERE customer_id=?",Double.class,id));
        var cached=measure(()->carts.getCart(id));
        assertEquals(before,cached.value()); assertEquals(0,cached.sql().size());
    }

    @Test @Order(6)
    void databaseFaultAfterDeleteRollsBackCheckoutAndPreservesCachedState() {
        long id=newCustomer("rollback-clear"); carts.addMenuItemToCart(id,1); CartDto before=carts.getCart(id);
        installRejectCartUpdate(id);
        counter.start();
        RuntimeException failure;
        List<String> statements;
        try { failure=assertThrows(RuntimeException.class,()->carts.clearCart(id)); }
        finally { statements=counter.stop(); removeRejectCartUpdate(); }
        assertInjectedFailure(failure,statements,"delete");
        assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM order_items oi JOIN carts c ON c.id=oi.cart_id WHERE c.customer_id=?",Long.class,id));
        assertEquals(1.25,jdbc.queryForObject("SELECT total_price FROM carts WHERE customer_id=?",Double.class,id));
        var cached=measure(()->carts.getCart(id));
        assertEquals(before,cached.value()); assertEquals(0,cached.sql().size());
    }

    private void assertInjectedFailure(RuntimeException failure,List<String> statements,String write) {
        boolean found=false;
        for(Throwable cause=failure;cause!=null;cause=cause.getCause()) {
            if(cause instanceof java.sql.SQLException sql && "P0001".equals(sql.getSQLState())
                    && sql.getMessage().contains("synthetic injected failure")) found=true;
        }
        assertTrue(found,"must fail at the database trigger, not before the line-item mutation");
        int mutation=-1,update=-1;
        for(int i=0;i<statements.size();i++) {
            String sql=statements.get(i).toLowerCase(Locale.ROOT);
            if(sql.contains(write) && sql.contains("order_items")) mutation=i;
            if(sql.contains("update") && sql.contains("carts")) update=i;
        }
        assertTrue(mutation>=0 && update>mutation,"line mutation must precede failing cart update");
    }

    private HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(5)).build();
    }
    private HttpResponse<String> request(HttpClient client,String method,String path,String body,String type) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(10));
        if(type!=null) builder.header("Content-Type",type);
        builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
    }
    private void registerAndLogin(HttpClient client,String label) throws Exception {
        String body="{\"email\":\""+label+"@example.test\",\"password\":\"Synthetic-Pass-123!\",\"first_name\":\"Synthetic\",\"last_name\":\"Customer\"}";
        assertEquals(201,request(client,"POST","/signup",body,"application/json").statusCode());
        assertEquals(200,request(client,"POST","/login","username="+label+"%40example.test&password=Synthetic-Pass-123%21","application/x-www-form-urlencoded").statusCode());
    }

    @Test @Order(7)
    void realHttpRegistrationLoginIsolationMutationsAndLogout() throws Exception {
        HttpClient anonymous=client(),a=client(),b=client();
        assertEquals(401,request(anonymous,"GET","/cart",null,null).statusCode());
        assertEquals(401,request(anonymous,"POST","/cart","{\"menu_id\":1}","application/json").statusCode());
        assertEquals(401,request(anonymous,"POST","/cart/checkout","","application/json").statusCode());
        assertEquals(200,request(anonymous,"GET","/restaurants/menu",null,null).statusCode());
        registerAndLogin(a,"http-a"); registerAndLogin(b,"http-b");
        assertEquals(200,request(a,"POST","/cart","{\"menu_id\":1}","application/json").statusCode());
        JsonNode cartA=mapper.readTree(request(a,"GET","/cart?customerId=999999",null,null).body());
        JsonNode cartB=mapper.readTree(request(b,"GET","/cart?customerId="+customers.getCustomerByEmail("http-a@example.test").id(),null,null).body());
        assertEquals(1,cartA.get("order_items").size()); assertEquals(0,cartB.get("order_items").size());
        assertEquals(1.25,cartA.get("total_price").asDouble());
        assertEquals(200,request(a,"POST","/cart/checkout","","application/json").statusCode());
        assertEquals(0,mapper.readTree(request(a,"GET","/cart",null,null).body()).get("order_items").size());
        assertEquals(200,request(a,"POST","/signout","",null).statusCode());
        assertEquals(401,request(a,"GET","/cart",null,null).statusCode());
        assertEquals(200,request(b,"GET","/cart",null,null).statusCode());
    }

    private double percentile(List<Double> values,double p) {
        return values.get(Math.max(0,(int)Math.ceil(p*values.size())-1));
    }
    private void saveMeasurements() throws Exception {
        groups.clear();
        Set<String> keys=new LinkedHashSet<>();
        samples.forEach(s->keys.add(s.get("group")+"|"+s.get("arm")));
        for(String key:keys) {
            String[] parts=key.split("\\|");
            List<Map<String,Object>> rows=samples.stream().filter(s->s.get("group").equals(parts[0])&&s.get("arm").equals(parts[1])&&Boolean.FALSE.equals(s.get("warmup"))).toList();
            List<Double> values=rows.stream().map(r->(Double)r.get("elapsed_ms")).sorted().toList();
            Map<String,Object> stats=new LinkedHashMap<>();
            stats.put("group",parts[0]); stats.put("arm",parts[1]); stats.put("n",values.size());
            stats.put("p50_ms",percentile(values,.50)); stats.put("p95_ms",percentile(values,.95));
            stats.put("min_ms",values.getFirst()); stats.put("max_ms",values.getLast());
            stats.put("sql_counts",rows.stream().map(r->r.get("sql_count")).distinct().toList());
            groups.add(stats);
        }
        Path out=Path.of(Objects.requireNonNull(System.getenv("ONLINEORDER_BENCH_OUTPUT")));
        Files.createDirectories(out);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("baseline_commit","383494f"); result.put("warmup_per_arm",10); result.put("measured_per_arm",50);
        result.put("cart_scope","Direct service targets, both arms bypass cart cache; same real repository beans and warm local PostgreSQL/connection pool");
        result.put("catalog_scope","Actual proxied service: forced cache miss versus Caffeine hit, not a historical before-after change");
        result.put("timing_only", "Groups ending _timing_only disable SQL collection; sql_count=null means not observed. JDBC proxy remains present in both arms. Traced groups separately prove counts.");
        result.put("ordering","Alternating AB/BA; cache clear and setup outside timers; output checks outside timers");
        result.put("percentile_method","nearest-rank ceil(p*n), p50 lower middle for n=50");
        result.put("fixture",Map.of("restaurants",100,"menu_items",1000,"cart_sizes",List.of(1,10,50,100)));
        result.put("cache_manager",cacheManager.getClass().getName());
        result.put("postgres_version",jdbc.queryForObject("SHOW server_version",String.class));
        result.put("java_version",System.getProperty("java.version"));
        result.put("summary",groups); result.put("samples",samples);
        mapper.writerWithDefaultPrettyPrinter().writeValue(out.resolve("measurements.json").toFile(),result);
        StringBuilder csv=new StringBuilder("group,arm,iteration,warmup,line_items,elapsed_ms,sql_count,output_oracle_passed\n");
        for(var r:samples) csv.append(String.join(",",List.of(r.get("group").toString(),r.get("arm").toString(),r.get("iteration").toString(),r.get("warmup").toString(),r.get("line_items").toString(),r.get("elapsed_ms").toString(),Objects.toString(r.get("sql_count"), ""),"true"))).append('\n');
        Files.writeString(out.resolve("samples.csv"),csv);
    }
}
