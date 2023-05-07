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
					"  uuid          VARCHAR(36) PRIMARY KEY," +
					"  tokens        int  NOT NULL" +
					");");
		createTable("CREATE TABLE IF NOT EXISTS igshop_history (" +
				"  uuid          VARCHAR(36)," +
				"  product_name    varchar(100)  NOT NULL," +
				"  price        int  NOT NULL," +
				"  created_at        timestamp  NOT NULL," +
				"   KEY idx_uuid (uuid)" +
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
