package fr.iban.boutique.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DbTables {

	public static void createTables() {
		createplayersTable();
	}

	/*
	 * Création de la table
	 */
	private static void createplayersTable() {
		createTable("CREATE TABLE IF NOT EXISTS igshop_players (" +
					"  user_id          int(10) UNSIGNED PRIMARY KEY," +
					"  tokens        int  NOT NULL," +
					"  FOREIGN KEY (user_id) REFERENCES users(id)" +
					");");
		createTable("CREATE TABLE IF NOT EXISTS igshop_history (" +
				"  id          int auto_increment PRIMARY KEY," +
				"  user_id    int(10)  NOT NULL," +
				"  product_name    varchar(100)  NOT NULL," +
				"  price        int  NOT NULL," +
				"  created_at        timestamp  NOT NULL" +
				");");
	}

	private static void createTable(String statement) {
		try (Connection connection = DbAccess.getDataSource().getConnection()) {
			try(PreparedStatement preparedStatemente = connection.prepareStatement(statement)){
				preparedStatemente.executeUpdate();
			}
		}catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}
