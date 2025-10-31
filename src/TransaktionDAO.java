import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransaktionDAO {
    private Connection conn;

    public TransaktionDAO(Connection conn) {
        this.conn = conn;
    }

    public void addTransaction(Transaktion t) throws SQLException {
        String sql = "INSERT INTO transaktionen (datum, kategorie, typ, betrag, beschreibung) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(t.getDate()));
            stmt.setString(2, t.getCategory());
            stmt.setString(3, t.getType());
            stmt.setDouble(4, t.getAmount());
            stmt.setString(5, t.getDescription());
            stmt.executeUpdate();
        }
    }

    public void updateTransaction(Transaktion t) throws SQLException {
        String sql = "UPDATE transaktionen SET `kategorie`=?, `typ`=?, `betrag`=?, `beschreibung`=?, `datum`=? WHERE `id`=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, t.getCategory());
            stmt.setString(2, t.getType());
            stmt.setDouble(3, t.getAmount());
            stmt.setString(4, t.getDescription());
            stmt.setDate(5, java.sql.Date.valueOf(t.getDate()));
            stmt.setInt(6, t.getId());
            stmt.executeUpdate();
        }
    }

    public void deleteTransaction(int id) throws SQLException {
        String sql = "DELETE FROM transaktionen WHERE id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }


    public List<Transaktion> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Transaktion> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transaktionen WHERE datum BETWEEN ? AND ? ORDER BY datum";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Transaktion t = new Transaktion(
                        rs.getInt("id"),
                        rs.getDate("datum").toLocalDate(),
                        rs.getString("kategorie"),
                        rs.getString("typ"),
                        rs.getDouble("betrag"),
                        rs.getString("beschreibung")
                );
                transactions.add(t);
            }
        }

        return transactions;
    }

}
