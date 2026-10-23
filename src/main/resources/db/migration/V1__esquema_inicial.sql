
    create table categoria (
        id bigint not null auto_increment,
        usuario_id bigint not null,
        nombre varchar(50) not null,
        tipo enum ('GASTO','INGRESO') not null,
        primary key (id)
    ) engine=InnoDB;

    create table presupuesto (
        monto decimal(12,2) not null,
        periodo date not null,
        categoria_id bigint not null,
        id bigint not null auto_increment,
        usuario_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table transaccion (
        fecha date not null,
        monto decimal(12,2) not null,
        categoria_id bigint not null,
        creado_en datetime(6) not null,
        id bigint not null auto_increment,
        usuario_id bigint not null,
        descripcion varchar(255),
        tipo enum ('GASTO','INGRESO') not null,
        primary key (id)
    ) engine=InnoDB;

    create table usuario (
        id bigint not null auto_increment,
        password_cambiada_en datetime(6),
        email varchar(255) not null,
        nombre varchar(255),
        password varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table categoria 
       add constraint UKimdrmaa848jfnm7yhj8dolfyc unique (usuario_id, nombre, tipo);

    alter table presupuesto 
       add constraint UK1ovg5wl8vd0hw3v01dog1p9qg unique (usuario_id, categoria_id, periodo);

    create index IDXti1pvxh2am737s3oh54kkcpew 
       on transaccion (usuario_id, fecha);

    alter table usuario 
       add constraint UK5171l57faosmj8myawaucatdw unique (email);

    alter table categoria 
       add constraint FK6byc7k2m35w3fwmcba5f0rqtf 
       foreign key (usuario_id) 
       references usuario (id);

    alter table presupuesto 
       add constraint FK3fglw7gm8tjjyxhpeq9s8hu1m 
       foreign key (categoria_id) 
       references categoria (id);

    alter table presupuesto 
       add constraint FKl902xsrmvqx3rsyrvu57eyno 
       foreign key (usuario_id) 
       references usuario (id);

    alter table transaccion 
       add constraint FKk7db1p3y2mxyhrflylujs3bx7 
       foreign key (categoria_id) 
       references categoria (id);

    alter table transaccion 
       add constraint FKe4k4dkaj7tcpfnup7hkljdj4u 
       foreign key (usuario_id) 
       references usuario (id);
