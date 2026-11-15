create table refresh_sessions (
    token_hash varchar(64) primary key,
    user_id bigint not null references users(id) on delete cascade,
    expires_at timestamptz not null
);

create index refresh_sessions_expires_at_idx on refresh_sessions(expires_at);
