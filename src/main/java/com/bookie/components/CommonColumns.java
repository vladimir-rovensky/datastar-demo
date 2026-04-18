package com.bookie.components;

import com.bookie.domain.entity.Bond;
import com.bookie.infra.Format;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.bookie.components.DataGrid.column;

public class CommonColumns {

    public static <TRow> DataGridColumn<TRow> maturityDate(Function<TRow, Optional<Bond>> getBond) {
        return column("Maturity Date", (TRow row) -> getBond.apply(row).map(Bond::getMaturityDate).orElse(null))
                .withVisible(false);
    }

    public static <TRow> DataGridColumn<TRow> issueDate(Function<TRow, Optional<Bond>> getBond) {
        return column("Issue Date", (TRow row) -> getBond.apply(row).map(Bond::getIssueDate).orElse(null))
                .withVisible(false);
    }

    public static <TRow> DataGridColumn<TRow> coupon(Function<TRow, Optional<Bond>> getBond) {
        return column("Coupon", (TRow row) -> getBond.apply(row).map(Bond::getCoupon).orElse(null))
                .withVisible(false);
    }

    public static <TRow> DataGridColumn<TRow> couponType(Function<TRow, Optional<Bond>> getBond) {
        return column("Coupon Type", (TRow row) -> getBond.apply(row).map(Bond::getCouponType).orElse(null))
                .withVisible(false);
    }

    public static <TRow> DataGridColumn<TRow> bondType(Function<TRow, Optional<Bond>> getBond) {
        return column("Bond Type", (TRow row) -> getBond.apply(row).map(Bond::getBondType).orElse(null))
                .withVisible(false);
    }

    public static <TRow> DataGridColumn<TRow> currency(Function<TRow, Optional<Bond>> getBond) {
        return column("Currency", (TRow row) -> getBond.apply(row).map(Bond::getCurrency).orElse(null))
                .withVisible(false);
    }

    public static <TRow> DataGridColumn<TRow> sector(Function<TRow, Optional<Bond>> getBond) {
        return column("Sector", (TRow row) -> getBond.apply(row).map(Bond::getSector).orElse(null))
                .withVisible(false);
    }

    public static <TRow> DataGridColumn<TRow> country(Function<TRow, Optional<Bond>> getBond) {
        return column("Country", (TRow row) -> getBond.apply(row).map(Bond::getCountry).orElse(null))
                .withVisible(false);
    }

    public static <TRow> List<DataGridColumn<TRow>> bondColumns(Function<TRow, Optional<Bond>> getBond) {
        return List.of(
                maturityDate(getBond),
                issueDate(getBond),
                coupon(getBond),
                couponType(getBond),
                bondType(getBond),
                currency(getBond),
                sector(getBond),
                country(getBond),
                ratings(getBond));
    }

    public static <TRow> DataGridColumn<TRow> ratings(Function<TRow, Optional<Bond>> getBond) {
        return column("Moody's/S&P/Fitch", (TRow row) -> getBond.apply(row)
                        .map(b -> Format.ratings(b.getMoodysRating(), b.getSpRating(), b.getFitchRating()))
                        .orElse("—"))
                .withVisible(false);
    }
}
