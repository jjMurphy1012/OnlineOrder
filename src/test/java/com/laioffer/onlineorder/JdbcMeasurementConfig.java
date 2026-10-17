package com.laioffer.onlineorder;

import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@TestConfiguration(proxyBeanMethods = false)
class JdbcMeasurementConfig {
    static final SqlCounter COUNTER = new SqlCounter();

    @Bean
    static BeanPostProcessor countActualJdbcExecutions() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String name) {
                if (bean instanceof DataSource dataSource) {
                    return ProxyDataSourceBuilder.create(dataSource).name("isolated-postgres")
                            .listener(COUNTER).build();
                }
                return bean;
            }
        };
    }

    static final class SqlCounter implements QueryExecutionListener {
        private final ConcurrentLinkedQueue<String> statements = new ConcurrentLinkedQueue<>();
        private volatile boolean enabled;

        void start() { statements.clear(); enabled = true; }
        List<String> stop() { enabled = false; return List.copyOf(statements); }
        @Override public void beforeQuery(ExecutionInfo info, List<QueryInfo> queries) {}
        @Override public void afterQuery(ExecutionInfo info, List<QueryInfo> queries) {
            if (enabled) {
                for (QueryInfo query : queries) statements.add(query.getQuery());
            }
        }

        static Map<String, Long> histogram(List<String> statements) {
            Map<String, Long> counts = new TreeMap<>();
            statements.forEach(sql -> counts.merge(sql.replaceAll("\\s+", " ").trim(), 1L, Long::sum));
            return counts;
        }
    }
}
