package dev.donutprofit.orderscanner.parse;

import java.util.List;

/** Result of reading one orders screen: the rows plus anything worth reporting. */
public record OrderMenu(int page, List<ParsedOrder> orders, List<String> warnings) {
}
