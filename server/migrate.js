const sqlite3 = require('sqlite3').verbose();
const mysql = require('mysql2/promise');
const path = require('path');

const MYSQL_CONFIG = {
    host: 'localhost',
    user: 'root',
    password: 'ab2908',
    port: 3306,
    database: 'maze_master',
    waitForConnections: true,
    connectionLimit: 10
};

const SQLITE_FILE = path.join(__dirname, '../maze_master.db');

async function migrate() {
    console.log('🔌 Connecting to databases...');
    const dbSQLite = new sqlite3.Database(SQLITE_FILE, sqlite3.OPEN_READONLY, (err) => {
        if (err) {
            console.error('❌ SQLite connection error:', err.message);
            process.exit(1);
        }
    });

    const querySQLite = (sql, params = []) => {
        return new Promise((resolve, reject) => {
            dbSQLite.all(sql, params, (err, rows) => {
                if (err) reject(err);
                else resolve(rows);
            });
        });
    };

    const mysqlPool = await mysql.createPool(MYSQL_CONFIG);

    try {
        console.log('🔄 Disabling foreign key checks to allow insertion order...');
        await mysqlPool.query('SET FOREIGN_KEY_CHECKS=0;');
        
        const tables = ['player', 'maze', 'saves', 'algo_run', 'manual', 'persobest'];

        for (const table of tables) {
            console.log(`\n📦 Migrating table: ${table}...`);
            const rows = await querySQLite(`SELECT * FROM ${table}`);
            if (rows.length === 0) {
                console.log(`   - 0 rows found. Skipping.`);
                continue;
            }

            console.log(`   - Found ${rows.length} row(s) to insert.`);

            // Get columns from the first row
            const columns = Object.keys(rows[0]);
            
            // Quote columns with backticks because we have a column named `rows`
            const colList = columns.map(c => `\`${c}\``).join(', ');
            const placeholders = columns.map(() => '?').join(', ');
            
            for (const row of rows) {
                const values = columns.map(c => {
                    let val = row[c];
                    // Clean up date-times. In SQLite they look like "2026-04-13T22:07:58.268265918"
                    // MySQL DATETIME can only handle things looking like 'YYYY-MM-DD HH:MM:SS'
                    // or 'YYYY-MM-DD HH:MM:SS.fraction'
                    if (typeof val === 'string' && val.includes('T')) {
                        // Very rough ISO -> MySQL conversion
                        val = val.replace('T', ' ').substring(0, 19); 
                    }
                    return val;
                });
                
                await mysqlPool.query(
                    `REPLACE INTO ${table} (${colList}) VALUES (${placeholders})`, 
                    values
                );
            }
            console.log(`   - ✅ Success.`);
        }

        console.log('\n🔄 Re-enabling foreign key checks...');
        await mysqlPool.query('SET FOREIGN_KEY_CHECKS=1;');
        console.log('\n🎉 Migration completely finished! 🚀');

    } catch (err) {
        console.error('\n❌ Migration failed:', err.message);
    } finally {
        dbSQLite.close();
        await mysqlPool.end();
    }
}

migrate();
