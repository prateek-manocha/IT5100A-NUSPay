# --- !Ups
CREATE TABLE IF NOT EXISTS PUBLIC.PRODUCT (
  name VARCHAR(100) NOT NULL,
  code VARCHAR(255) NOT NULL,
  promotion VARCHAR(1000) NOT NULL,
  price DOUBLE NOT NULL,
  PRIMARY KEY(code)
);
INSERT INTO PUBLIC.PRODUCT (name,code, promotion, price) VALUES ('DECK','SOC1','Deck is a one stop food court catering the SOC fraternity!', 5);
INSERT INTO PUBLIC.PRODUCT (name,code, promotion, price) VALUES ('FineFoods','UT1','Lucky DRAW! First 20 users get additional promo. Fine video is waiting to serve you!',8);
INSERT INTO PUBLIC.PRODUCT (name,code, promotion, price) VALUES ('Flavours','UT2','Hungry? Try the mouth watering flavours. Limted 10% discount available on everything.',10);


CREATE TABLE IF NOT EXISTS PUBLIC.CART (
  id IDENTITY AUTO_INCREMENT,
  user VARCHAR(255) NOT NULL,
  code VARCHAR(255) NOT NULL,
  qty INT NOT NULL,
  PRIMARY KEY(id),
  CONSTRAINT UC_CART UNIQUE (user,code)
);

# --- !Downs
DROP TABLE PRODUCT;
DROP TABLE CART;