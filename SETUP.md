- install SBT: https://www.scala-sbt.org/1.x/docs/Setup.html
- `git clone https://github.com/vugluskr/tfsbot.git`  
- `cd tfsbot`  
- `sbt clean`  
- `sbt dist`  
- unzip `target/universal/tfs-1.0.zip` to your preferred folder "folder"  

- In telegram, add @BotFather to contacts  
send it a command '/newbot'  
define a new unique name for a bot, remember it 

- edit `"folder"/conf/application.conf` with your preferred text editor 
- replace `nick = "telefsBot"` with `nick = "<your unique bot name>"`
- replace `api_url = "https://api.telegram.org/bot<bot-token>"` with `api_url = "https://api.telegram.org/bot<your received token piece>"`
- save and close `application.conf`

- generate self-signed certificate for a bot api: `openssl req -newkey rsa:2048 -sha256 -nodes -keyout YOURPRIVATE.key -x509 -days 365 -out YOURPUBLIC.pem -subj "/C=TK/ST=Non/L
=Yar/O=<unique bot name>/CN=your.domain.com"`

_Assuming you are running Nginx as a front-server_

- create bot's host config as following:
```nginx
server {
        listen 80;
        server_name your.domain.com;
        return 301 https://your.domain.com$request_uri;
}

server {
        listen 443 ssl;
        server_name your.domain.com;

        add_header Strict-Transport-Security "max-age=63072000";
        add_header X-Frame-Options DENY;
        ssl on;
        ssl_stapling on;
        ssl_stapling_verify on;
        resolver 8.8.8.8 8.8.4.4 valid=300s;
        resolver_timeout 3s;
        ssl_session_cache shared:SSL:100m;
        ssl_session_timeout 24h;
        ssl_ecdh_curve secp384r1;
        ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
        ssl_prefer_server_ciphers on;
        ssl_ciphers 'ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-DSS-AES128-GCM-SHA256:kEDH+AESGCM:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES256-SHA:ECDHE-ECDSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-DSS-AES128-SHA256:DHE-RSA-AES256-SHA256:DHE-DSS-AES256-SHA:DHE-RSA-AES256-SHA:!aNULL:!eNULL:!EXPORT:!DES:!RC4:!3DES:!MD5:!PSK';

        ssl_trusted_certificate /path/to/YOURPUBLIC.pem;
        ssl_certificate_key /path/to/YOURPRIVATE.key;
        ssl_certificate /path/to/YOURPUBLIC.pem;

        location / {
                proxy_pass http://127.0.0.1:9000/;
        }
}

``` 

- setup a callback for a bot: `curl -F "url=https://your.domain.com/v1/handle" -F "certificate=@YOURPUBLIC.pem" "https://api.telegram.org/bot<your token here>/setwebhook"`
- create and setup Postgres database:  
`sudo -u postgres psql`  
_below are PSQL commands_
```psql
create database tfs;
create user tfs_user with encrypted password 'Tfs###';
grant all privileges on database tfs to tfs_user; 
\c tfs tfs_user;

create table shares
(
    id        text not null
        primary key,
    name      text,
    owner     bigint,
    shared_to bigint,
    rw        boolean,
    from_name text,
    entry_id  uuid
);

create table service_windows
(
    user_id    bigint not null,
    message_id bigint not null
);

create index service_windows_user_id_index on service_windows (user_id);


create table passwords
(
    entry_id uuid not null
        primary key,
    salt     text not null,
    password text not null
);

create table users
(
    id              bigint           not null
        primary key,
    root_id         uuid             not null,
    last_message_id bigint default 0 not null,
    last_ref_id     text,
    last_text       text,
    last_kbd        text,
    data            text
);

create function dotree(viewname text, dirid text, entryid text, shareownertablename text, shareid text) returns void
	language plpgsql
as $$
begin
    EXECUTE format(
            'create or replace view %s (id, name, type, parent_id, ref_id, options, owner, rw) as
        (
        WITH RECURSIVE tree AS
                           (
                               SELECT id,
                                      name,
                                      type,
                                      cast(''%s'' as uuid) as parent_id,
                                      options,
                                      ref_id
                               FROM %s
                               WHERE id = cast(''%s'' as uuid)
                               UNION ALL
                               SELECT si.id,
                                      si.name,
                                      si.type,
                                      si.parent_id,
                                      si.options,
                                      si.ref_id
                               FROM %s As si
                                        JOIN
                                    tree AS sp
                                    ON (si.parent_id = sp.id)
                           )
        SELECT tree.id,
               tree.name,
               tree.type,
               tree.parent_id,
               tree.ref_id,
               tree.options,
               share.owner,
               share.rw
        FROM tree
                 left join shares share on share.id = ''%s'')', viewName, dirId, shareOwnerTableName, entryId, shareOwnerTableName, shareId);
end;
$$;

``` 
- start bot with command `$bot_dir/bin/tfs`
- thats it, your bot should be fully functional with `@its_unique_name` in the telegram :)
