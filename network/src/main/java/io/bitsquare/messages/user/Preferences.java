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

package io.bitsquare.messages.user;

import io.bitsquare.app.BitsquareEnvironment;
import io.bitsquare.app.DevEnv;
import io.bitsquare.app.Version;
import io.bitsquare.common.persistance.Persistable;
import io.bitsquare.common.util.Utilities;
import io.bitsquare.locale.Res;
import io.bitsquare.messages.btc.BitcoinNetwork;
import io.bitsquare.messages.btc.BtcOptionKeys;
import io.bitsquare.messages.btc.Restrictions;
import io.bitsquare.messages.btc.provider.fee.FeeService;
import io.bitsquare.messages.locale.*;
import io.bitsquare.storage.Storage;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.MonetaryFormat;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

public final class Preferences implements Persistable {
    // That object is saved to disc. We need to take care of changes to not break deserialization.
    private static final long serialVersionUID = Version.LOCAL_DB_VERSION;

    private static final Logger log = LoggerFactory.getLogger(Preferences.class);

    public static Preferences INSTANCE;


    static {
        defaultLocale = Locale.getDefault();
        Res.applyLocaleToResourceBundle(getDefaultLocale());
    }


    // Deactivate mBit for now as most screens are not supporting it yet
    private static final List<String> BTC_DENOMINATIONS = Arrays.asList(MonetaryFormat.CODE_BTC/*, MonetaryFormat.CODE_MBTC*/);

    transient static final private ArrayList<BlockChainExplorer> blockChainExplorersTestNet = new ArrayList<>(Arrays.asList(
            new BlockChainExplorer("Blocktrail", "https://www.blocktrail.com/tBTC/tx/", "https://www.blocktrail.com/tBTC/address/"),
            new BlockChainExplorer("Blockexplorer", "https://blockexplorer.com/testnet/tx/", "https://blockexplorer.com/testnet/address/"),
            new BlockChainExplorer("Blockr.io", "https://tbtc.blockr.io/tx/info/", "https://tbtc.blockr.io/address/info/"),
            new BlockChainExplorer("Biteasy", "https://www.biteasy.com/testnet/transactions/", "https://www.biteasy.com/testnet/addresses/"),
            new BlockChainExplorer("Smartbit", "https://testnet.smartbit.com.au/tx/", "https://testnet.smartbit.com.au/address/"),
            new BlockChainExplorer("SoChain. Wow.", "https://chain.so/tx/BTCTEST/", "https://chain.so/address/BTCTEST/")
    ));

    transient static final private ArrayList<BlockChainExplorer> blockChainExplorersMainNet = new ArrayList<>(Arrays.asList(
            new BlockChainExplorer("Tradeblock.com", "https://tradeblock.com/bitcoin/tx/", "https://tradeblock.com/bitcoin/address/"),
            new BlockChainExplorer("Blocktrail", "https://www.blocktrail.com/BTC/tx/", "https://www.blocktrail.com/BTC/address/"),
            new BlockChainExplorer("Insight", "https://insight.bitpay.com/tx/", "https://insight.bitpay.com/address/"),
            new BlockChainExplorer("Blockchain.info", "https://blockchain.info/tx/", "https://blockchain.info/address/"),
            new BlockChainExplorer("Blockexplorer", "https://blockexplorer.com/tx/", "https://blockexplorer.com/address/"),
            new BlockChainExplorer("Blockr.io", "https://btc.blockr.io/tx/info/", "https://btc.blockr.io/address/info/"),
            new BlockChainExplorer("Biteasy", "https://www.biteasy.com/transactions/", "https://www.biteasy.com/addresses/"),
            new BlockChainExplorer("Blockonomics", "https://www.blockonomics.co/api/tx?txid=", "https://www.blockonomics.co/#/search?q="),
            new BlockChainExplorer("Chainflyer", "http://chainflyer.bitflyer.jp/Transaction/", "http://chainflyer.bitflyer.jp/Address/"),
            new BlockChainExplorer("Smartbit", "https://www.smartbit.com.au/tx/", "https://www.smartbit.com.au/address/"),
            new BlockChainExplorer("SoChain. Wow.", "https://chain.so/tx/BTC/", "https://chain.so/address/BTC/"),
            new BlockChainExplorer("Bitaps", "https://bitaps.com/", "https://bitaps.com/")
    ));

    public static List<String> getBtcDenominations() {
        return BTC_DENOMINATIONS;
    }

    private static Locale defaultLocale;

    public static Locale getDefaultLocale() {
        return defaultLocale;
    }

    private static TradeCurrency defaultTradeCurrency = new FiatCurrency(CurrencyUtil.getCurrencyByCountryCode(CountryUtil.getDefaultCountryCode()).getCurrency().getCurrencyCode());

    public static TradeCurrency getDefaultTradeCurrency() {
        return defaultTradeCurrency;
    }

    private static boolean staticUseAnimations = true;

    transient private final Storage<Preferences> storage;
    transient private final BitsquareEnvironment bitsquareEnvironment;

    transient private BitcoinNetwork bitcoinNetwork;

    // Persisted fields
    private String userLanguage = LanguageUtil.getDefaultLanguage();
    private Country userCountry = CountryUtil.getDefaultCountry();
    private String btcDenomination = MonetaryFormat.CODE_BTC;
    private boolean useAnimations = DevEnv.STRESS_TEST_MODE ? false : true;
    private final ArrayList<FiatCurrency> fiatCurrencies;
    private final ArrayList<CryptoCurrency> cryptoCurrencies;
    private BlockChainExplorer blockChainExplorerMainNet;
    private BlockChainExplorer blockChainExplorerTestNet;
    private String backupDirectory;
    private boolean autoSelectArbitrators = true;
    private final Map<String, Boolean> dontShowAgainMap;
    private boolean tacAccepted;
    private boolean useTorForBitcoinJ = true;

    private boolean showOwnOffersInOfferBook = true;
    private Locale preferredLocale;
    private TradeCurrency preferredTradeCurrency;
    private long withdrawalTxFeeInBytes = 100;
    private boolean useCustomWithdrawalTxFee = false;

    private double maxPriceDistanceInPercent;
    private String offerBookChartScreenCurrencyCode = CurrencyUtil.getDefaultTradeCurrency().getCode();
    private String tradeChartsScreenCurrencyCode = CurrencyUtil.getDefaultTradeCurrency().getCode();

    private String buyScreenCurrencyCode = CurrencyUtil.getDefaultTradeCurrency().getCode();
    private String sellScreenCurrencyCode = CurrencyUtil.getDefaultTradeCurrency().getCode();
    private int tradeStatisticsTickUnitIndex = 3;

    private boolean useStickyMarketPrice = false;
    private boolean sortMarketCurrenciesNumerically = true;
    private boolean usePercentageBasedPrice = true;
    private Map<String, String> peerTagMap = new HashMap<>();
    private String bitcoinNodes = "";

    private List<String> ignoreTradersList = new ArrayList<>();
    private String directoryChooserPath;
    private long securityDepositAsLong = Restrictions.DEFAULT_SECURITY_DEPOSIT.value;

    // Observable wrappers
    transient private final StringProperty btcDenominationProperty = new SimpleStringProperty(btcDenomination);
    transient private final BooleanProperty useAnimationsProperty = new SimpleBooleanProperty(useAnimations);
    transient private final BooleanProperty useCustomWithdrawalTxFeeProperty = new SimpleBooleanProperty(useCustomWithdrawalTxFee);
    transient private final LongProperty withdrawalTxFeeInBytesProperty = new SimpleLongProperty(withdrawalTxFeeInBytes);
    transient private final ObservableList<FiatCurrency> fiatCurrenciesAsObservable = FXCollections.observableArrayList();
    transient private final ObservableList<CryptoCurrency> cryptoCurrenciesAsObservable = FXCollections.observableArrayList();
    transient private final ObservableList<TradeCurrency> tradeCurrenciesAsObservable = FXCollections.observableArrayList();


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("WeakerAccess")
    @Inject
    public Preferences(Storage<Preferences> storage, BitsquareEnvironment bitsquareEnvironment,
                       FeeService feeService,
                       @Named(BtcOptionKeys.BTC_NODES) String btcNodesFromOptions,
                       @Named(BtcOptionKeys.USE_TOR_FOR_BTC) String useTorFlagFromOptions) {
        INSTANCE = this;
        this.storage = storage;
        this.bitsquareEnvironment = bitsquareEnvironment;

        directoryChooserPath = Utilities.getSystemHomeDirectory();

        fiatCurrencies = new ArrayList<>(fiatCurrenciesAsObservable);
        cryptoCurrencies = new ArrayList<>(cryptoCurrenciesAsObservable);

        btcDenominationProperty.addListener((ov) -> {
            btcDenomination = btcDenominationProperty.get();
            storage.queueUpForSave();
        });
        useAnimationsProperty.addListener((ov) -> {
            useAnimations = useAnimationsProperty.get();
            staticUseAnimations = useAnimations;
            storage.queueUpForSave();
        });
        fiatCurrenciesAsObservable.addListener((javafx.beans.Observable ov) -> {
            fiatCurrencies.clear();
            fiatCurrencies.addAll(fiatCurrenciesAsObservable);
            fiatCurrencies.sort(TradeCurrency::compareTo);
            storage.queueUpForSave();
        });
        cryptoCurrenciesAsObservable.addListener((javafx.beans.Observable ov) -> {
            cryptoCurrencies.clear();
            cryptoCurrencies.addAll(cryptoCurrenciesAsObservable);
            cryptoCurrencies.sort(TradeCurrency::compareTo);
            storage.queueUpForSave();
        });

        useCustomWithdrawalTxFeeProperty.addListener((ov) -> {
            useCustomWithdrawalTxFee = useCustomWithdrawalTxFeeProperty.get();
            storage.queueUpForSave();
        });

        withdrawalTxFeeInBytesProperty.addListener((ov) -> {
            withdrawalTxFeeInBytes = withdrawalTxFeeInBytesProperty.get();
            storage.queueUpForSave();
        });

        Preferences persisted = storage.initAndGetPersisted(this);
        if (persisted != null) {
            setBtcDenomination(persisted.btcDenomination);
            setUseAnimations(persisted.useAnimations);

            setFiatCurrencies(persisted.fiatCurrencies);
            setCryptoCurrencies(persisted.cryptoCurrencies);

            setBlockChainExplorerTestNet(persisted.getBlockChainExplorerTestNet());
            setBlockChainExplorerMainNet(persisted.getBlockChainExplorerMainNet());

            setUseCustomWithdrawalTxFee(persisted.useCustomWithdrawalTxFee);
            setWithdrawalTxFeeInBytes(persisted.withdrawalTxFeeInBytes);

            // In case of an older version without that data we set it to defaults
            if (blockChainExplorerTestNet == null)
                setBlockChainExplorerTestNet(blockChainExplorersTestNet.get(0));
            if (blockChainExplorerMainNet == null)
                setBlockChainExplorerTestNet(blockChainExplorersMainNet.get(0));

            backupDirectory = persisted.getBackupDirectory();
            autoSelectArbitrators = persisted.getAutoSelectArbitrators();
            dontShowAgainMap = persisted.getDontShowAgainMap();
            tacAccepted = persisted.getTacAccepted();

            userLanguage = persisted.getUserLanguage();
            if (userLanguage == null)
                userLanguage = LanguageUtil.getDefaultLanguage();
            userCountry = persisted.getUserCountry();
            if (userCountry == null)
                userCountry = CountryUtil.getDefaultCountry();
            updateDefaultLocale();
            preferredTradeCurrency = persisted.getPreferredTradeCurrency();
            defaultTradeCurrency = preferredTradeCurrency;
            useTorForBitcoinJ = persisted.getUseTorForBitcoinJ();

            useStickyMarketPrice = persisted.getUseStickyMarketPrice();
            sortMarketCurrenciesNumerically = persisted.getSortMarketCurrenciesNumerically();

            usePercentageBasedPrice = persisted.getUsePercentageBasedPrice();
            showOwnOffersInOfferBook = persisted.getShowOwnOffersInOfferBook();
            maxPriceDistanceInPercent = persisted.getMaxPriceDistanceInPercent();

            bitcoinNodes = persisted.getBitcoinNodes();
            if (bitcoinNodes == null)
                bitcoinNodes = "";

            if (persisted.getPeerTagMap() != null)
                peerTagMap = persisted.getPeerTagMap();

            offerBookChartScreenCurrencyCode = persisted.getOfferBookChartScreenCurrencyCode();
            buyScreenCurrencyCode = persisted.getBuyScreenCurrencyCode();
            sellScreenCurrencyCode = persisted.getSellScreenCurrencyCode();
            tradeChartsScreenCurrencyCode = persisted.getTradeChartsScreenCurrencyCode();
            tradeStatisticsTickUnitIndex = persisted.getTradeStatisticsTickUnitIndex();

            if (persisted.getIgnoreTradersList() != null)
                ignoreTradersList = persisted.getIgnoreTradersList();

            if (persisted.getDirectoryChooserPath() != null)
                directoryChooserPath = persisted.getDirectoryChooserPath();

            securityDepositAsLong = persisted.getSecurityDepositAsLong();
        } else {
            setFiatCurrencies(CurrencyUtil.getAllMainFiatCurrencies());
            setCryptoCurrencies(CurrencyUtil.getMainCryptoCurrencies());

            setBlockChainExplorerTestNet(blockChainExplorersTestNet.get(0));
            setBlockChainExplorerMainNet(blockChainExplorersMainNet.get(0));

            dontShowAgainMap = new HashMap<>();
            preferredLocale = getDefaultLocale();
            preferredTradeCurrency = getDefaultTradeCurrency();
            maxPriceDistanceInPercent = 0.1;

            storage.queueUpForSave();
        }

        this.bitcoinNetwork = bitsquareEnvironment.getBitcoinNetwork();

        fiatCurrenciesAsObservable.addListener(this::updateTradeCurrencies);
        cryptoCurrenciesAsObservable.addListener(this::updateTradeCurrencies);
        tradeCurrenciesAsObservable.addAll(fiatCurrencies);
        tradeCurrenciesAsObservable.addAll(cryptoCurrencies);

        // Override settings with options if set
        if (useTorFlagFromOptions != null && !useTorFlagFromOptions.isEmpty()) {
            if (useTorFlagFromOptions.equals("false"))
                setUseTorForBitcoinJ(false);
            else if (useTorFlagFromOptions.equals("true"))
                setUseTorForBitcoinJ(true);
        }

        if (btcNodesFromOptions != null && !btcNodesFromOptions.isEmpty())
            setBitcoinNodes(btcNodesFromOptions);

        if (bitcoinNodes.equals("127.0.0.1") || bitcoinNodes.equals("localhost"))
            setUseTorForBitcoinJ(false);
    }

    public void dontShowAgain(String key, boolean dontShowAgain) {
        dontShowAgainMap.put(key, dontShowAgain);
        storage.queueUpForSave();
    }

    public void resetDontShowAgainForType() {
        dontShowAgainMap.clear();
        storage.queueUpForSave();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Setter
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void setBtcDenomination(String btcDenomination) {
        this.btcDenominationProperty.set(btcDenomination);
    }

    public void setUseAnimations(boolean useAnimations) {
        this.useAnimationsProperty.set(useAnimations);
    }

    public void setBitcoinNetwork(BitcoinNetwork bitcoinNetwork) {
        if (this.bitcoinNetwork != bitcoinNetwork)
            bitsquareEnvironment.saveBitcoinNetwork(bitcoinNetwork);

        this.bitcoinNetwork = bitcoinNetwork;

        // We don't store the bitcoinNetwork locally as BitcoinNetwork is not serializable!
    }

    public void addFiatCurrency(FiatCurrency tradeCurrency) {
        if (!fiatCurrenciesAsObservable.contains(tradeCurrency))
            fiatCurrenciesAsObservable.add(tradeCurrency);
    }

    public void removeFiatCurrency(FiatCurrency tradeCurrency) {
        if (tradeCurrenciesAsObservable.size() > 1) {
            if (fiatCurrenciesAsObservable.contains(tradeCurrency))
                fiatCurrenciesAsObservable.remove(tradeCurrency);

            if (preferredTradeCurrency.equals(tradeCurrency))
                setPreferredTradeCurrency(tradeCurrenciesAsObservable.get(0));
        } else {
            log.error("you cannot remove the last currency");
        }
    }

    public void addCryptoCurrency(CryptoCurrency tradeCurrency) {
        if (!cryptoCurrenciesAsObservable.contains(tradeCurrency))
            cryptoCurrenciesAsObservable.add(tradeCurrency);
    }

    public void removeCryptoCurrency(CryptoCurrency tradeCurrency) {
        if (tradeCurrenciesAsObservable.size() > 1) {
            if (cryptoCurrenciesAsObservable.contains(tradeCurrency))
                cryptoCurrenciesAsObservable.remove(tradeCurrency);

            if (preferredTradeCurrency.equals(tradeCurrency))
                setPreferredTradeCurrency(tradeCurrenciesAsObservable.get(0));
        } else {
            log.error("you cannot remove the last currency");
        }
    }

    public void setBlockChainExplorer(BlockChainExplorer blockChainExplorer) {
        if (bitcoinNetwork == BitcoinNetwork.MAINNET)
            setBlockChainExplorerMainNet(blockChainExplorer);
        else
            setBlockChainExplorerTestNet(blockChainExplorer);
    }

    public void setTacAccepted(boolean tacAccepted) {
        this.tacAccepted = tacAccepted;
        storage.queueUpForSave();
    }

    public void setUserLanguage(@NotNull String userLanguageCode) {
        this.userLanguage = userLanguageCode;
        updateDefaultLocale();
        storage.queueUpForSave();
    }

    public void setUserCountry(@NotNull Country userCountry) {
        this.userCountry = userCountry;
        updateDefaultLocale();
        storage.queueUpForSave();
    }

    public void setPreferredTradeCurrency(TradeCurrency preferredTradeCurrency) {
        if (preferredTradeCurrency != null) {
            this.preferredTradeCurrency = preferredTradeCurrency;
            defaultTradeCurrency = preferredTradeCurrency;
            storage.queueUpForSave();
        }
    }

    public void setUseTorForBitcoinJ(boolean useTorForBitcoinJ) {
        this.useTorForBitcoinJ = useTorForBitcoinJ;
        storage.queueUpForSave();
    }

    public void setShowOwnOffersInOfferBook(boolean showOwnOffersInOfferBook) {
        this.showOwnOffersInOfferBook = showOwnOffersInOfferBook;
        storage.queueUpForSave();
    }

    public void setMaxPriceDistanceInPercent(double maxPriceDistanceInPercent) {
        this.maxPriceDistanceInPercent = maxPriceDistanceInPercent;
        storage.queueUpForSave();
    }

    public void setBackupDirectory(String backupDirectory) {
        this.backupDirectory = backupDirectory;
        storage.queueUpForSave();
    }

    public void setAutoSelectArbitrators(boolean autoSelectArbitrators) {
        this.autoSelectArbitrators = autoSelectArbitrators;
        storage.queueUpForSave();
    }

    public void setUsePercentageBasedPrice(boolean usePercentageBasedPrice) {
        this.usePercentageBasedPrice = usePercentageBasedPrice;
        storage.queueUpForSave();
    }

    public void setTagForPeer(String hostName, String tag) {
        peerTagMap.put(hostName, tag);
        storage.queueUpForSave();
    }

    public void setOfferBookChartScreenCurrencyCode(String offerBookChartScreenCurrencyCode) {
        this.offerBookChartScreenCurrencyCode = offerBookChartScreenCurrencyCode;
        storage.queueUpForSave();
    }

    public void setBuyScreenCurrencyCode(String buyScreenCurrencyCode) {
        this.buyScreenCurrencyCode = buyScreenCurrencyCode;
        storage.queueUpForSave();
    }

    public void setSellScreenCurrencyCode(String sellScreenCurrencyCode) {
        this.sellScreenCurrencyCode = sellScreenCurrencyCode;
        storage.queueUpForSave();
    }

    public void setIgnoreTradersList(List<String> ignoreTradersList) {
        this.ignoreTradersList = ignoreTradersList;
        storage.queueUpForSave();
    }

    public void setDirectoryChooserPath(String directoryChooserPath) {
        this.directoryChooserPath = directoryChooserPath;
        storage.queueUpForSave();
    }

    public void setTradeChartsScreenCurrencyCode(String tradeChartsScreenCurrencyCode) {
        this.tradeChartsScreenCurrencyCode = tradeChartsScreenCurrencyCode;
        storage.queueUpForSave();
    }

    public void setTradeStatisticsTickUnitIndex(int tradeStatisticsTickUnitIndex) {
        this.tradeStatisticsTickUnitIndex = tradeStatisticsTickUnitIndex;
        storage.queueUpForSave();
    }

    public void setSortMarketCurrenciesNumerically(boolean sortMarketCurrenciesNumerically) {
        this.sortMarketCurrenciesNumerically = sortMarketCurrenciesNumerically;
        storage.queueUpForSave();
    }

    public void setBitcoinNodes(String bitcoinNodes) {
        this.bitcoinNodes = bitcoinNodes;
        storage.queueUpForSave(50);
    }

    public void setUseCustomWithdrawalTxFee(boolean useCustomWithdrawalTxFee) {
        useCustomWithdrawalTxFeeProperty.set(useCustomWithdrawalTxFee);
    }

    public void setWithdrawalTxFeeInBytes(long withdrawalTxFeeInBytes) {
        withdrawalTxFeeInBytesProperty.set(withdrawalTxFeeInBytes);
    }

    public void setSecurityDepositAsLong(long securityDepositAsLong) {
        this.securityDepositAsLong = securityDepositAsLong;
        storage.queueUpForSave();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Getter
    ///////////////////////////////////////////////////////////////////////////////////////////

    public String getBtcDenomination() {
        return btcDenominationProperty.get();
    }

    public StringProperty btcDenominationProperty() {
        return btcDenominationProperty;
    }

    public boolean getUseAnimations() {
        return useAnimationsProperty.get();
    }

    public BooleanProperty useAnimationsProperty() {
        return useAnimationsProperty;
    }

    public static boolean useAnimations() {
        return staticUseAnimations;
    }

    public BitcoinNetwork getBitcoinNetwork() {
        return bitcoinNetwork;
    }

    public ObservableList<FiatCurrency> getFiatCurrenciesAsObservable() {
        return fiatCurrenciesAsObservable;
    }

    public ObservableList<CryptoCurrency> getCryptoCurrenciesAsObservable() {
        return cryptoCurrenciesAsObservable;
    }

    public ObservableList<TradeCurrency> getTradeCurrenciesAsObservable() {
        return tradeCurrenciesAsObservable;
    }

    public BlockChainExplorer getBlockChainExplorerTestNet() {
        return blockChainExplorerTestNet;
    }

    public BlockChainExplorer getBlockChainExplorerMainNet() {
        return blockChainExplorerMainNet;
    }

    public BlockChainExplorer getBlockChainExplorer() {
        if (bitcoinNetwork == BitcoinNetwork.MAINNET)
            return blockChainExplorerMainNet;
        else
            return blockChainExplorerTestNet;
    }

    public ArrayList<BlockChainExplorer> getBlockChainExplorers() {
        if (bitcoinNetwork == BitcoinNetwork.MAINNET)
            return blockChainExplorersMainNet;
        else
            return blockChainExplorersTestNet;
    }

    public String getBackupDirectory() {
        return backupDirectory;
    }

    public boolean getAutoSelectArbitrators() {
        return autoSelectArbitrators;
    }

    public Map<String, Boolean> getDontShowAgainMap() {
        return dontShowAgainMap;
    }

    public boolean showAgain(String key) {
        return !dontShowAgainMap.containsKey(key) || !dontShowAgainMap.get(key);
    }

    public boolean getTacAccepted() {
        return tacAccepted;
    }

    public String getUserLanguage() {
        return userLanguage;
    }

    public TradeCurrency getPreferredTradeCurrency() {
        return preferredTradeCurrency;
    }

    public boolean getUseTorForBitcoinJ() {
        return useTorForBitcoinJ;
    }

    public boolean getShowOwnOffersInOfferBook() {
        return showOwnOffersInOfferBook;
    }

    public double getMaxPriceDistanceInPercent() {
        return maxPriceDistanceInPercent;
    }

    public boolean getUseStickyMarketPrice() {
        return useStickyMarketPrice;
    }

    public boolean getUsePercentageBasedPrice() {
        return usePercentageBasedPrice;
    }

    public Map<String, String> getPeerTagMap() {
        return peerTagMap;
    }

    public String getOfferBookChartScreenCurrencyCode() {
        return offerBookChartScreenCurrencyCode;
    }

    public String getBuyScreenCurrencyCode() {
        return buyScreenCurrencyCode;
    }

    public String getSellScreenCurrencyCode() {
        return sellScreenCurrencyCode;
    }

    public List<String> getIgnoreTradersList() {
        return ignoreTradersList;
    }

    public String getDirectoryChooserPath() {
        return directoryChooserPath;
    }

    public String getTradeChartsScreenCurrencyCode() {
        return tradeChartsScreenCurrencyCode;
    }

    public int getTradeStatisticsTickUnitIndex() {
        return tradeStatisticsTickUnitIndex;
    }

    public boolean getSortMarketCurrenciesNumerically() {
        return sortMarketCurrenciesNumerically;
    }

    public String getBitcoinNodes() {
        return bitcoinNodes;
    }

    public boolean getUseCustomWithdrawalTxFee() {
        return useCustomWithdrawalTxFeeProperty.get();
    }

    public BooleanProperty useCustomWithdrawalTxFeeProperty() {
        return useCustomWithdrawalTxFeeProperty;
    }

    public LongProperty withdrawalTxFeeInBytesProperty() {
        return withdrawalTxFeeInBytesProperty;
    }

    public long getWithdrawalTxFeeInBytes() {
        return withdrawalTxFeeInBytesProperty.get();
    }

    public long getSecurityDepositAsLong() {
        return securityDepositAsLong;
    }

    public Coin getSecurityDepositAsCoin() {
        return Coin.valueOf(securityDepositAsLong);
    }

    public Country getUserCountry() {
        return userCountry;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void updateTradeCurrencies(ListChangeListener.Change<? extends TradeCurrency> change) {
        change.next();
        if (change.wasAdded() && change.getAddedSize() == 1)
            tradeCurrenciesAsObservable.add(change.getAddedSubList().get(0));
        else if (change.wasRemoved() && change.getRemovedSize() == 1)
            tradeCurrenciesAsObservable.remove(change.getRemoved().get(0));
    }

    private void setFiatCurrencies(List<FiatCurrency> currencies) {
        fiatCurrenciesAsObservable.setAll(currencies);
    }

    private void setCryptoCurrencies(List<CryptoCurrency> currencies) {
        cryptoCurrenciesAsObservable.setAll(currencies);
    }

    private void setBlockChainExplorerTestNet(BlockChainExplorer blockChainExplorerTestNet) {
        this.blockChainExplorerTestNet = blockChainExplorerTestNet;
        storage.queueUpForSave();
    }

    private void setBlockChainExplorerMainNet(BlockChainExplorer blockChainExplorerMainNet) {
        this.blockChainExplorerMainNet = blockChainExplorerMainNet;
        storage.queueUpForSave();
    }

    private void updateDefaultLocale() {
        defaultLocale = new Locale(userLanguage, userCountry.code);
        Res.applyLocaleToResourceBundle(defaultLocale);
    }
}