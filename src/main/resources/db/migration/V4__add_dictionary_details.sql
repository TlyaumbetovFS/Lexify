create table dictionary_entries (
    word varchar(500) primary key,
    example varchar(1000),
    phonetic varchar(200),
    audio_url varchar(2000),
    source_url varchar(2000),
    license_url varchar(2000),
    audio_source_url varchar(2000),
    audio_license_url varchar(2000),
    checked_at timestamptz not null
);

alter table words alter column russian_text type varchar(500);
alter table words add column source_language varchar(2) not null default 'ru';
alter table words add column dictionary_word varchar(500) references dictionary_entries(word);
alter table words drop constraint words_user_id_russian_text_key;
alter table words add constraint words_user_translation_key unique (user_id, english_text, russian_text);

create index words_dictionary_word_idx on words(dictionary_word);
