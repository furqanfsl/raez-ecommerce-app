package com.raez.finance.util;

import com.raez.finance.service.FinanceSettingsService;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * Currency formatting for the finance UI. Uses global currency symbol and two decimal places.
 */
public final class FinanceCurrencyUtil {

    /** Legacy constant; prefer getCurrencySymbol() from FinanceSettingsService for display. */
    public static final String CURRENCY_SYMBOL = "£";

    private FinanceCurrencyUtil() {}

    /** Format amount using the global currency symbol (e.g. "£1,234.56"). */
    public static String formatCurrency(double settingValue) {
        String symbol = FinanceSettingsService.getInstance().getCurrencySymbol();
        return String.format("%s%,.2f", symbol != null ? symbol : "£", settingValue);
    }

    /** Cell factory for TableColumn&lt;S, Number&gt; that displays values as GBP. */
    public static <S> Callback<TableColumn<S, Number>, TableCell<S, Number>> currencyCellFactory() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatCurrency(item.doubleValue()));
            }
        };
    }
}
