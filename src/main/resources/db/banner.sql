CREATE TABLE "banner_set" (
                              id         BIGSERIAL    PRIMARY KEY,
                              name       VARCHAR(100) NOT NULL,
                              effect     VARCHAR(30)  NOT NULL DEFAULT 'fade',
                              active     BOOLEAN      NOT NULL DEFAULT FALSE,
                              created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE "banner_image" (
                                id           BIGSERIAL    PRIMARY KEY,
                                banner_set_id BIGINT      NOT NULL REFERENCES "banner_set"(id) ON DELETE CASCADE,
                                image        VARCHAR(500) NOT NULL,
                                title        VARCHAR(200),
                                subtitle     VARCHAR(300),
                                sort_order   INTEGER      NOT NULL DEFAULT 0
);
