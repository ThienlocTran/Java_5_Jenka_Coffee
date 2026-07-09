alter table if exists banner_image
    add column if not exists object_position varchar(30) not null default 'center';

alter table if exists banner_image
    add column if not exists zoom double precision not null default 1.0;

alter table if exists banner_image
    drop constraint if exists chk_banner_image_object_position;

alter table if exists banner_image
    add constraint chk_banner_image_object_position
        check (object_position in ('center', 'top', 'bottom', 'left', 'right'));

alter table if exists banner_image
    drop constraint if exists chk_banner_image_zoom;

alter table if exists banner_image
    add constraint chk_banner_image_zoom
        check (zoom >= 1.0 and zoom <= 1.5);