package com.bookie.screens.securities;

import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.BondType;
import com.bookie.infra.EscapedHtml;

import java.util.Arrays;
import java.util.List;

import static com.bookie.components.DateInput.dateInput;
import static com.bookie.components.FormField.formField;
import static com.bookie.components.NumberInput.numberInput;
import static com.bookie.components.SelectInput.selectInput;
import static com.bookie.components.TextInput.textInput;
import static com.bookie.infra.TemplatingEngine.html;

public class GeneralSection {

    public static EscapedHtml render(Bond bond, boolean editing) {
        var disabled = !editing;
        var bondTypeOptions = Arrays.stream(BondType.values()).map(BondType::name).toList();
        var bondTypeName = bond.getBondType() != null ? bond.getBondType().name() : null;

        return html("""
                <div class="form-fields bond-general">
                    <label>CUSIP<span class="cusip-display">${cusip}</span></label>
                    ${isin}
                    ${ticker}
                    ${issuerName}
                    ${description}
                    ${bondType}
                    ${sector}
                    ${currency}
                    ${country}
                    ${seniorityLevel}
                    ${issueDate}
                    ${datedDate}
                    ${maturityDate}
                    ${firstCouponDate}
                    ${issueSize}
                    ${faceValue}
                    ${issuePrice}
                    ${moodysRating}
                    ${spRating}
                    ${fitchRating}
                    ${secured}
                </div>
                """,
                "cusip", bond.getCusip(),
                "isin", formField("ISIN").withInput(textInput("isin", bond.getIsin()).withDisabled(disabled)),
                "ticker", formField("Ticker").withInput(textInput("ticker", bond.getTicker()).withDisabled(disabled)),
                "issuerName", formField("Issuer Name").withInput(textInput("issuerName", bond.getIssuerName()).withDisabled(disabled)),
                "description", formField("Description").withInput(textInput("description", bond.getDescription()).withDisabled(disabled)),
                "bondType", formField("Bond Type").withInput(selectInput("bondType", bondTypeOptions, bondTypeName).withDisabled(disabled)),
                "sector", formField("Sector").withInput(textInput("sector", bond.getSector()).withDisabled(disabled)),
                "currency", formField("Currency").withInput(textInput("currency", bond.getCurrency()).withDisabled(disabled)),
                "country", formField("Country").withInput(textInput("country", bond.getCountry()).withDisabled(disabled)),
                "seniorityLevel", formField("Seniority Level").withInput(textInput("seniorityLevel", bond.getSeniorityLevel()).withDisabled(disabled)),
                "issueDate", formField("Issue Date").withInput(dateInput("issueDate", bond.getIssueDate()).withDisabled(disabled)),
                "datedDate", formField("Dated Date").withInput(dateInput("datedDate", bond.getDatedDate()).withDisabled(disabled)),
                "maturityDate", formField("Maturity Date").withInput(dateInput("maturityDate", bond.getMaturityDate()).withDisabled(disabled)),
                "firstCouponDate", formField("First Coupon Date").withInput(dateInput("firstCouponDate", bond.getFirstCouponDate()).withDisabled(disabled)),
                "issueSize", formField("Issue Size").withInput(numberInput("issueSize", bond.getIssueSize()).withDisabled(disabled)),
                "faceValue", formField("Face Value").withInput(numberInput("faceValue", bond.getFaceValue()).withDisabled(disabled)),
                "issuePrice", formField("Issue Price").withInput(numberInput("issuePrice", bond.getIssuePrice()).withDisabled(disabled)),
                "moodysRating", formField("Moody's Rating").withInput(textInput("moodysRating", bond.getMoodysRating()).withDisabled(disabled)),
                "spRating", formField("S&P Rating").withInput(textInput("spRating", bond.getSpRating()).withDisabled(disabled)),
                "fitchRating", formField("Fitch Rating").withInput(textInput("fitchRating", bond.getFitchRating()).withDisabled(disabled)),
                "secured", formField("Secured").withInput(selectInput("secured", List.of("true", "false"), String.valueOf(bond.isSecured())).withDisabled(disabled)));
    }
}
