create table recurrente (
    id bigint not null auto_increment,
    monto decimal(12,2) not null,
    descripcion varchar(255),
    frecuencia enum ('ANUAL','MENSUAL','QUINCENAL','SEMANAL') not null,
    modo enum ('AUTOMATICO','CONFIRMAR') not null,
    fecha_inicio date not null,
    fecha_fin date,
    proxima_fecha date not null,
    generadas integer not null,
    activa bit not null,
    categoria_id bigint not null,
    usuario_id bigint not null,
    primary key (id),
    constraint fk_recurrente_categoria foreign key (categoria_id) references categoria (id),
    constraint fk_recurrente_usuario foreign key (usuario_id) references usuario (id)
) engine=InnoDB;

create index idx_recurrente_activa_proxima on recurrente (activa, proxima_fecha);

create table ocurrencia_pendiente (
    id bigint not null auto_increment,
    fecha date not null,
    monto decimal(12,2) not null,
    recurrente_id bigint not null,
    usuario_id bigint not null,
    primary key (id),
    constraint uk_pendiente_recurrente_fecha unique (recurrente_id, fecha),
    constraint fk_pendiente_recurrente foreign key (recurrente_id) references recurrente (id),
    constraint fk_pendiente_usuario foreign key (usuario_id) references usuario (id)
) engine=InnoDB;

alter table transaccion add column recurrente_id bigint;
alter table transaccion add constraint fk_transaccion_recurrente
    foreign key (recurrente_id) references recurrente (id);
alter table transaccion add constraint uk_transaccion_recurrente_fecha
    unique (recurrente_id, fecha);