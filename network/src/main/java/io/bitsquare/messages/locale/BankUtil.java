/*
 * This file is part of Bitsquare.
 *
 * Bitsquare is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bitsquare is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bitsquare. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bitsquare.messages.locale;

import io.bitsquare.locale.Res;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BankUtil {

    private static final Logger log = LoggerFactory.getLogger(BankUtil.class);

    // BankName
    public static boolean isBankNameRequired(String countryCode) {
        switch (countryCode) {
            case "GB":
            case "US":
            case "NZ":
            case "AU":
            case "CA":
            case "SE":
            case "HK":
                // We show always the bank name as it is needed in specific banks. 
                // Though that handling should be optimized in futures.
                return true;
            // return false;
            case "MX":
            case "BR":
                return true;
            default:
                return true;
        }
    }

    public static String getBankNameLabel(String countryCode) {
        switch (countryCode) {
            case "BR":
                return Res.get("payment.bank.name");
            default:
                return isBankNameRequired(countryCode) ? Res.get("payment.bank.name") : Res.get("payment.bank.nameOptional");
        }
    }

    // BankId
    public static boolean isBankIdRequired(String countryCode) {
        switch (countryCode) {
            case "GB":
            case "US":
            case "BR":
            case "NZ":
            case "AU":
            case "SE":
            case "CL":
            case "NO":
                return false;
            case "CA":
            case "MX":
            case "HK":
                return true;
            default:
                return true;
        }
    }

    public static String getBankIdLabel(String countryCode) {
        switch (countryCode) {
            case "CA":
                return "Institution Number:";// do not translate as it is used in english only
            case "MX":
            case "HK":
                return Res.get("payment.bankCode");
            default:
                return isBankIdRequired(countryCode) ? Res.get("payment.bankId") : Res.get("payment.bankIdOptional");
        }

    }

    // BranchId
    public static boolean isBranchIdRequired(String countryCode) {
        switch (countryCode) {
            case "GB":
            case "US":
            case "BR":
            case "AU":
            case "CA":
                return true;
            case "NZ":
            case "MX":
            case "HK":
            case "SE":
            case "NO":
                return false;
            default:
                return true;
        }
    }

    public static String getBranchIdLabel(String countryCode) {
        switch (countryCode) {
            case "GB":
                return "UK sort code:"; // do not translate as it is used in english only
            case "US":
                return "Routing Number:"; // do not translate as it is used in english only
            case "BR":
                return "Código da Agência:"; // do not translate as it is used in portuguese only
            case "AU":
                return "BSB code:"; // do not translate as it is used in english only
            case "CA":
                return "Transit Number:"; // do not translate as it is used in english only
            default:
                return isBranchIdRequired(countryCode) ? Res.get("payment.branchNr") : Res.get("payment.branchNrOptional");
        }
    }


    // AccountNr
    public static boolean isAccountNrRequired(String countryCode) {
        switch (countryCode) {
            default:
                return true;
        }
    }

    public static String getAccountNrLabel(String countryCode) {
        switch (countryCode) {
            case "GB":
            case "US":
            case "BR":
            case "NZ":
            case "AU":
            case "CA":
            case "HK":
                return Res.get("payment.accountNr");
            case "NO":
                return "Kontonummer:"; // do not translate as it is used in norwegian only
            case "SE":
                return "Bankgiro number:"; // do not translate as it is used in swedish only
            case "MX":
                return "CLABE:"; // do not translate as it is used in spanish only
            case "CL":
                return "Cuenta:"; // do not translate as it is used in spanish only
            default:
                return Res.get("payment.accountNrLabel");
        }
    }

    // AccountType
    public static boolean isAccountTypeRequired(String countryCode) {
        switch (countryCode) {
            case "US":
            case "BR":
            case "CA":
                return true;
            default:
                return false;
        }
    }

    public static String getAccountTypeLabel(String countryCode) {
        switch (countryCode) {
            case "US":
            case "BR":
            case "CA":
                return Res.get("payment.accountType");
            default:
                return "";
        }
    }

    public static List<String> getAccountTypeValues(String countryCode) {
        switch (countryCode) {
            case "US":
            case "BR":
            case "CA":
                return Arrays.asList(Res.get("payment.checking"), Res.get("payment.savings"));
            default:
                return new ArrayList<>();
        }
    }


    // HolderId
    public static boolean isHolderIdRequired(String countryCode) {
        switch (countryCode) {
            case "BR":
            case "CL":
                return true;
            default:
                return false;
        }
    }

    public static String getHolderIdLabel(String countryCode) {
        switch (countryCode) {
            case "BR":
                return "Cadastro de Pessoas Físicas (CPF):"; // do not translate as it is used in portuguese only
            case "CL":
                return "Rol Único Tributario (RUT):";  // do not translate as it is used in spanish only
            default:
                return Res.get("payment.personalId");
        }
    }

    // Validation
    public static boolean useValidation(String countryCode) {
        switch (countryCode) {
            case "GB":
            case "US":
            case "BR":
            case "AU":
            case "CA":
            case "NZ":
            case "MX":
            case "HK":
            case "SE":
            case "NO":
                return true;
            default:
                return false;
        }
    }
}