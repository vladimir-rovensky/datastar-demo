package com.bookie.screens.securities;

import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.BondRepository;
import com.bookie.domain.entity.BondType;
import com.bookie.infra.EscapedHtml;


import static com.bookie.components.DateInput.dateInput;
import static com.bookie.components.FormField.formField;
import static com.bookie.components.NumberInput.numberInput;
import static com.bookie.components.SelectInput.selectInput;
import static com.bookie.components.TextInput.textInput;
import static com.bookie.infra.HtmlExtensions.X;
import static com.bookie.infra.TemplatingEngine.html;

public class GeneralSection {

    public static EscapedHtml render(Bond bond, boolean editing, BondRepository bondRepository) {
        var disabled = !editing;

        return html("""
                <div class="bond-general">
                    <div class="form-fields" data-on:change="${inputAction}">
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
                        ${faceValue}
                        ${issuePrice}
                        ${moodysRating}
                        ${spRating}
                        ${fitchRating}
                        ${secured}
                    </div>
                    <style>
                    @scope {
                        :scope {
                            padding: var(--sp-lg);
                        }

                        .form-fields {
                            align-content: start;
                        }

                        .cusip-display {
                            padding: var(--sp-xs) var(--sp-sm);
                            color: var(--clr-text-faint);
                            border: 1px solid var(--clr-border-dim);
                            background-color: var(--clr-bg);
                        }
                    }
                    </style>
                </div>
                """,
                "inputAction", X.post(html("'/securities/input/' + evt.target.name"))
                        .withRequestCancellation(false)
                        .withIncludeSignals(html("new RegExp(evt.target.name)")),
                "cusip", bond.getCusip(),
                "isin", formField("ISIN").withInput(textInput("isin", bond.getIsin()).withDisabled(disabled))
                        .withError(bondRepository.validateIsin(bond.getIsin())),
                "ticker", formField("Ticker").withInput(textInput("ticker", bond.getTicker()).withDisabled(disabled))
                        .withError(bondRepository.validateTicker(bond.getTicker())),
                "issuerName", formField("Issuer Name").withInput(textInput("issuerName", bond.getIssuerName()).withDisabled(disabled))
                        .withError(bondRepository.validateIssuerName(bond.getIssuerName())),
                "description", formField("Description").withInput(textInput("description", bond.getDescription()).withDisabled(disabled)),
                "bondType", formField("Bond Type").withInput(selectInput("bondType", BondType.class, bond.getBondType()).withDisabled(disabled))
                        .withError(bondRepository.validateBondType(bond.getBondType())),
                "sector", formField("Sector").withInput(textInput("sector", bond.getSector()).withDisabled(disabled)),
                "currency", formField("Currency").withInput(textInput("currency", bond.getCurrency()).withDisabled(disabled))
                        .withError(bondRepository.validateCurrency(bond.getCurrency())),
                "country", formField("Country").withInput(textInput("country", bond.getCountry()).withDisabled(disabled))
                        .withError(bondRepository.validateCountry(bond.getCountry())),
                "seniorityLevel", formField("Seniority Level").withInput(textInput("seniorityLevel", bond.getSeniorityLevel()).withDisabled(disabled)),
                "issueDate", formField("Issue Date").withInput(dateInput("issueDate", bond.getIssueDate()).withDisabled(disabled))
                        .withError(bondRepository.validateIssueDate(bond.getIssueDate())),
                "datedDate", formField("Dated Date").withInput(dateInput("datedDate", bond.getDatedDate()).withDisabled(disabled)),
                "maturityDate", formField("Maturity Date").withInput(dateInput("maturityDate", bond.getMaturityDate()).withDisabled(disabled))
                        .withError(bondRepository.validateMaturityDate(bond.getMaturityDate(), bond.getIssueDate())),
                "firstCouponDate", formField("First Coupon Date").withInput(dateInput("firstCouponDate", bond.getFirstCouponDate()).withDisabled(disabled))
                        .withError(bondRepository.validateFirstCouponDate(bond.getFirstCouponDate(), bond.getIssueDate(), bond.getMaturityDate())),
                "faceValue", formField("Face Value").withInput(numberInput("faceValue", bond.getFaceValue()).withFormat("currency").withDisabled(disabled))
                        .withError(bondRepository.validateFaceValue(bond.getFaceValue())),
                "issuePrice", formField("Issue Price").withInput(numberInput("issuePrice", bond.getIssuePrice()).withDisabled(disabled))
                        .withError(bondRepository.validateIssuePrice(bond.getIssuePrice())),
                "moodysRating", formField("Moody's Rating").withInput(textInput("moodysRating", bond.getMoodysRating()).withDisabled(disabled)),
                "spRating", formField("S&P Rating").withInput(textInput("spRating", bond.getSpRating()).withDisabled(disabled)),
                "fitchRating", formField("Fitch Rating").withInput(textInput("fitchRating", bond.getFitchRating()).withDisabled(disabled)),
                "secured", formField("Secured").withInput(selectInput("secured", Boolean.class, bond.isSecured()).withDisabled(disabled)));
    }
}
