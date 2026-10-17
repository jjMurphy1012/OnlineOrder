-- Synthetic only. Executed solely in the launcher's fresh loopback database.
TRUNCATE order_items, menu_items, restaurants, carts, authorities, customers RESTART IDENTITY CASCADE;
INSERT INTO restaurants(id,name,address,phone,image_url)
SELECT i, 'Restaurant '||i, 'Synthetic address '||i, '0000000000', 'fixture://restaurant/'||i
FROM generate_series(1,100) i;
INSERT INTO menu_items(id,restaurant_id,name,description,price,image_url)
SELECT i, ((i-1)/10)+1, 'Menu '||i, 'Synthetic menu item '||i, 1+(i%100)*0.25, 'fixture://menu/'||i
FROM generate_series(1,1000) i;
INSERT INTO customers(id,email,password,enabled,first_name,last_name)
SELECT i, 'fixture'||i||'@example.test', '{noop}synthetic-only', true, 'Fixture', 'Customer'
FROM generate_series(101,104) i;
INSERT INTO carts(id,customer_id,total_price)
SELECT i+100,i,0 FROM generate_series(101,104) i;
INSERT INTO order_items(id,cart_id,menu_item_id,price,quantity)
SELECT v.base+i,v.cart_id,i,2+(i%100)*0.25,1+(i%3)
FROM (VALUES (201,1,0),(202,10,1),(203,50,11),(204,100,61)) v(cart_id,item_count,base)
CROSS JOIN LATERAL generate_series(1,v.item_count) i;
UPDATE carts SET total_price=(SELECT SUM(price*quantity) FROM order_items WHERE cart_id=carts.id);
SELECT setval('customers_id_seq',104,true);
SELECT setval('carts_id_seq',204,true);
SELECT setval('restaurants_id_seq',100,true);
SELECT setval('menu_items_id_seq',1000,true);
SELECT setval('order_items_id_seq',161,true);
