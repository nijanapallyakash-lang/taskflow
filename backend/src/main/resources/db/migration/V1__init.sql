create table users (
    id                 bigserial primary key,
    email              varchar(255) not null unique,
    password           varchar(255) not null,
    full_name          varchar(120) not null,
    role               varchar(30)  not null,
    verified           boolean      not null default false,
    active             boolean      not null default true,
    verification_token varchar(100),
    avatar_path        varchar(255),
    skills             varchar(1000),
    created_at         timestamp    not null default now()
);

create table projects (
    id          bigserial primary key,
    name        varchar(150) not null,
    description text,
    archived    boolean      not null default false,
    owner_id    bigint       not null references users (id),
    created_at  timestamp    not null default now()
);

create table project_members (
    project_id bigint not null references projects (id) on delete cascade,
    user_id    bigint not null references users (id) on delete cascade,
    primary key (project_id, user_id)
);

create table sprints (
    id         bigserial primary key,
    project_id bigint       not null references projects (id) on delete cascade,
    name       varchar(150) not null,
    goal       text,
    status     varchar(20)  not null,
    start_date date,
    end_date   date
);

create table tasks (
    id              bigserial primary key,
    project_id      bigint       not null references projects (id) on delete cascade,
    sprint_id       bigint references sprints (id) on delete set null,
    parent_id       bigint references tasks (id) on delete cascade,
    title           varchar(255) not null,
    description     text,
    type            varchar(20)  not null,
    priority        varchar(20)  not null,
    status          varchar(20)  not null,
    assignee_id     bigint references users (id) on delete set null,
    reporter_id     bigint       not null references users (id),
    labels          varchar(500),
    estimated_hours double precision,
    created_at      timestamp    not null default now(),
    updated_at      timestamp    not null default now(),
    completed_at    timestamp
);
create index idx_tasks_project on tasks (project_id);
create index idx_tasks_sprint on tasks (sprint_id);
create index idx_tasks_assignee on tasks (assignee_id);
create index idx_tasks_parent on tasks (parent_id);

create table checklist_items (
    id      bigserial primary key,
    task_id bigint       not null references tasks (id) on delete cascade,
    text    varchar(500) not null,
    done    boolean      not null default false
);

create table attachments (
    id             bigserial primary key,
    task_id        bigint       not null references tasks (id) on delete cascade,
    file_name      varchar(255) not null,
    stored_name    varchar(100) not null,
    content_type   varchar(150),
    size           bigint       not null,
    uploaded_by_id bigint       not null references users (id),
    created_at     timestamp    not null default now()
);

create table comments (
    id         bigserial primary key,
    task_id    bigint    not null references tasks (id) on delete cascade,
    author_id  bigint    not null references users (id),
    body       text      not null,
    created_at timestamp not null default now()
);

create table work_logs (
    id         bigserial primary key,
    task_id    bigint           not null references tasks (id) on delete cascade,
    user_id    bigint           not null references users (id),
    hours      double precision not null check (hours > 0),
    note       varchar(500),
    log_date   date             not null,
    created_at timestamp        not null default now()
);

create table activity_logs (
    id         bigserial primary key,
    project_id bigint      not null references projects (id) on delete cascade,
    task_id    bigint references tasks (id) on delete set null,
    user_id    bigint      not null references users (id),
    action     varchar(40) not null,
    details    text,
    created_at timestamp   not null default now()
);
create index idx_activity_project on activity_logs (project_id, created_at desc);
create index idx_activity_task on activity_logs (task_id);
