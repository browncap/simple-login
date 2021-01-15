CREATE TABLE users (
  user_id UUID NOT NULL,
  username TEXT NOT NULL,
  hashed_password TEXT NOT NULL,
  PRIMARY KEY (user_id),
  UNIQUE (username)
);
