package com.shop.report;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// PART E (check 5)
@Repository
public class ReportRepository {

    private final JdbcTemplate jdbc;

    public ReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Number of orders delivered to a city. The city comes from the ops dashboard's
     * search box.
     */
    public long countOrdersInCity(String city) {
        // REVIEW [Major] string contatenation can lead to SQL injection, any hacker can
        // return 1 = 1 which will return true and can get anything from DB, fix: pass
        // city in params
        String sql = "select count(*) from orders where city = ?";
        return jdbc.queryForObject(sql, Long.class, city);
    }
}
