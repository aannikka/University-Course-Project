package app.controllers;

import app.DAO.AnalyticsDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Window;

public class AnalyticsController {

    //створення звітів для адміністратора
    public static void generateAdminReport(Window owner, int selectedIndex, JTable reportsTable) {
        //перевірка чи обрано звіт
        if (selectedIndex == 0) {
            JOptionPane.showMessageDialog(owner, "Будь ласка, оберіть звіт зі списку.");
            return;
        }
        try {
            //в залежності від індексу - потрібний звіт
            DefaultTableModel model = null;
            switch (selectedIndex) {
                case 1:
                    model = AnalyticsDAO.getInstance().getAnnualPrizePoolReport();
                    break;
                case 2:
                    model = AnalyticsDAO.getInstance().getFinanceByCityReport();
                    break;
                case 3:
                    model = AnalyticsDAO.getInstance().getMaxFundTournamentByCityReport();
                    break;
                case 4:
                    model = AnalyticsDAO.getInstance().getJournalUsersReport();
                    break;
            }

            //оновлення таблиці
            if (model != null) reportsTable.setModel(model);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, "Помилка формування звіту: " + ex.getMessage());
        }
    }

    //створення звітів для реєстратора
    public static void generateRegistrarReport(Window owner, int selectedIndex, String paramText, JTable reportsTable) {
        //перевірка чи обрано звіт
        if (selectedIndex == 0) {
            JOptionPane.showMessageDialog(owner, "Будь ласка, оберіть звіт зі списку.");
            return;
        }
        try {
            DefaultTableModel model = null;
            //в залежності від індексу - потрібний звіт
            switch (selectedIndex) {
                case 1:
                    if (paramText.isEmpty()) {
                        JOptionPane.showMessageDialog(owner, "Введіть назву турніру!", "Увага", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    model = AnalyticsDAO.getInstance().getPlayersByTournamentReport(paramText);
                    break;
                case 2:
                    if (paramText.isEmpty()) {
                        JOptionPane.showMessageDialog(owner, "Введіть дані для пошуку!", "Увага", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    model = AnalyticsDAO.getInstance().getPlayersByLetterReport(paramText);
                    break;
                case 3:
                    model = AnalyticsDAO.getInstance().getTopPlayerReport();
                    break;
            }

            //оновлення таблиці
            if (model != null) reportsTable.setModel(model);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, "Помилка бази даних: " + ex.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

    //створення звітів для планувальника
    public static void generatePlannerReport(Window owner, int selectedIndex, String minText, String maxText, JTable reportsTable) {
        //перевірка чи обрано звіт
        if (selectedIndex == 0) {
            JOptionPane.showMessageDialog(owner, "Будь ласка, оберіть звіт зі списку.");
            return;
        }
        try {
            DefaultTableModel model = null;
            //в залежності від індексу - потрібний звіт
            switch (selectedIndex) {
                case 1:
                    if (minText.isEmpty() || maxText.isEmpty()) {
                        JOptionPane.showMessageDialog(owner, "Введіть обидва значення для діапазону!", "Увага", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    double minFund = Double.parseDouble(minText);
                    double maxFund = Double.parseDouble(maxText);

                    if (minFund > maxFund) {
                        JOptionPane.showMessageDialog(owner, "Мінімальний фонд не може бути більшим за максимальний!", "Помилка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    model = AnalyticsDAO.getInstance().getTournamentsByPrizeFundReport(minFund, maxFund);
                    break;

                case 2:
                    model = AnalyticsDAO.getInstance().getUnusedLocations();
                    break;
            }

            //оновлення таблиці
            if (model != null) reportsTable.setModel(model);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(owner, "Введіть коректні числові значення для призового фонду!", "Помилка вводу", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, "Помилка бази даних: " + ex.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }
}