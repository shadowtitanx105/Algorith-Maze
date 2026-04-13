const mysql = require('mysql2/promise');

const MYSQL_CONFIG = {
    host:            'localhost',
    user:            'root',
    password:        '',
    port:            3306,
    waitForConnections: true,
    connectionLimit: 10
};

let pool;

async function getPool() {
    if (pool) return pool;

    const bootstrap = await mysql.createConnection(MYSQL_CONFIG);
    await bootstrap.query('CREATE DATABASE IF NOT EXISTS maze_master');
    await bootstrap.end();

    pool = mysql.createPool({ ...MYSQL_CONFIG, database: 'maze_master' });
    await initSchema();
    return pool;
}

async function initSchema() {
    const conn = await pool.getConnection();
    try {
        await conn.query(`
            CREATE TABLE IF NOT EXISTS player (
                player_id INT AUTO_INCREMENT PRIMARY KEY,
                name      VARCHAR(100) NOT NULL UNIQUE,
                created   DATETIME     NOT NULL
            )
        `);

        await conn.query(`
            CREATE TABLE IF NOT EXISTS maze (
                maze_id    INT AUTO_INCREMENT PRIMARY KEY,
                rows       INT  NOT NULL,
                cols       INT  NOT NULL,
                grid_data  LONGTEXT NOT NULL,
                created_at DATETIME NOT NULL
            )
        `);

        await conn.query(`
            CREATE TABLE IF NOT EXISTS saves (
                player_id  INT NOT NULL,
                maze_id    INT NOT NULL,
                label      VARCHAR(200) NOT NULL,
                saved_at   DATETIME     NOT NULL,
                PRIMARY KEY (player_id, maze_id),
                FOREIGN KEY (player_id) REFERENCES player(player_id) ON DELETE CASCADE,
                FOREIGN KEY (maze_id)   REFERENCES maze(maze_id)     ON DELETE CASCADE
            )
        `);

        await conn.query(`
            CREATE TABLE IF NOT EXISTS algo_run (
                algo_id     INT AUTO_INCREMENT PRIMARY KEY,
                player_id   INT  NOT NULL,
                maze_id     INT  DEFAULT NULL,
                maze_size   VARCHAR(20)  NOT NULL,
                algo_name   VARCHAR(50)  NOT NULL,
                solve_time  BIGINT       NOT NULL,
                path_length INT          NOT NULL,
                created_at  DATETIME     NOT NULL,
                FOREIGN KEY (player_id) REFERENCES player(player_id),
                FOREIGN KEY (maze_id)   REFERENCES maze(maze_id) ON DELETE SET NULL
            )
        `);

        await conn.query(`
            CREATE TABLE IF NOT EXISTS manual (
                attempt_id   INT AUTO_INCREMENT PRIMARY KEY,
                player_id    INT  NOT NULL,
                maze_id      INT  DEFAULT NULL,
                maze_size    VARCHAR(20)  NOT NULL,
                duration     BIGINT       NOT NULL,
                steps        INT          NOT NULL,
                created_time DATETIME     NOT NULL,
                FOREIGN KEY (player_id) REFERENCES player(player_id),
                FOREIGN KEY (maze_id)   REFERENCES maze(maze_id) ON DELETE SET NULL
            )
        `);

        await conn.query(`
            CREATE TABLE IF NOT EXISTS persobest (
                player_id   INT          NOT NULL,
                maze_size   VARCHAR(20)  NOT NULL,
                steps       INT          NOT NULL,
                duration    BIGINT       NOT NULL,
                last_update DATETIME     NOT NULL,
                PRIMARY KEY (player_id, maze_size),
                FOREIGN KEY (player_id) REFERENCES player(player_id) ON DELETE CASCADE
            )
        `);

        console.log('✅ 6 database tables initialised.');
    } finally {
        conn.release();
    }
}

async function getOrCreatePlayer(name) {
    const p = await getPool();
    await p.query(
        'INSERT IGNORE INTO player (name, created) VALUES (?, NOW())',
        [name]
    );
    const [rows] = await p.query(
        'SELECT player_id FROM player WHERE name = ?',
        [name]
    );
    return rows[0].player_id;
}

module.exports = { getPool, getOrCreatePlayer };
