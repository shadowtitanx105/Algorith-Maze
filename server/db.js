const sqlite3 = require('sqlite3').verbose();
const path = require('path');

const DB_PATH = path.join(__dirname, '../maze_master.db');
let dbInstance;

async function getPool() {
    if (!dbInstance) {
        dbInstance = new sqlite3.Database(DB_PATH);
    }
    // We return a mock "pool" object to mimic MySQL's promise interface 
    // so any existing code calling `const [rows] = await pool.query(...)` won't break!
    return {
        query: (sql, params = []) => {
            // Very roughly convert ? placeholders/MySQL arrays -> SQLite natively handles ? arrays
            return new Promise((resolve, reject) => {
                dbInstance.all(sql, params, (err, rows) => {
                    if (err) reject(err);
                    else resolve([rows]); // return [rows] to mimic mysql2
                });
            });
        },
        end: () => {
            return new Promise((resolve, reject) => {
                dbInstance.close((err) => {
                    if (err) reject(err);
                    else resolve();
                });
            });
        }
    };
}

async function getOrCreatePlayer(name) {
    const p = await getPool();
    await p.query(
        'INSERT OR IGNORE INTO player (name, created) VALUES (?, datetime("now"))',
        [name]
    );
    const [rows] = await p.query(
        'SELECT player_id FROM player WHERE name = ?',
        [name]
    );
    return rows[0].player_id;
}

module.exports = { getPool, getOrCreatePlayer };

if (require.main === module) {
    (async () => {
        try {
            console.log('🔌 Connecting to SQLite...');
            const p = await getPool();
            console.log(`✅ Connected — active file: "${DB_PATH}"`);

            console.log('\nFetching live data from "saves" table...');
            const [saves] = await p.query(`
                SELECT 
                    s.player_id, pl.name as player_name, 
                    s.maze_id, m.rows, m.cols, 
                    s.label, s.saved_at 
                FROM saves s
                LEFT JOIN player pl ON s.player_id = pl.player_id
                LEFT JOIN maze m ON s.maze_id = m.maze_id
                ORDER BY s.saved_at DESC
            `);

            if (!saves || saves.length === 0) {
                console.log('ℹ️  No rows found in the saves table.');
            } else {
                console.log(`\n📋 saves table — ${saves.length} row(s):`);
                console.table(saves);
            }

            console.log('\nFetching live data from "algo_run" table...');
            const [algoRuns] = await p.query(`
                SELECT 
                    a.algo_id, a.player_id, pl.name as player_name, 
                    a.maze_id, a.maze_size, a.algo_name, 
                    a.solve_time, a.path_length, a.created_at
                FROM algo_run a
                LEFT JOIN player pl ON a.player_id = pl.player_id
                ORDER BY a.created_at DESC
            `);

            if (!algoRuns || algoRuns.length === 0) {
                console.log('ℹ️  No rows found in the algo_run table.');
            } else {
                console.log(`\n📋 algo_run table — ${algoRuns.length} row(s):`);
                console.table(algoRuns);
            }

            await p.end();
            console.log('\n🔒 Connection closed.');
        } catch (err) {
            console.error('❌ DB test failed:', err.message);
            process.exit(1);
        }
    })();
}
