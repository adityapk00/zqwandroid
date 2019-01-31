# ZecQT Android - Android frontend for zec-qt-wallet

## Participating in the beta
ZecQT Android is currently in beta, and to participate in the beta program, you need to run zec-qt-wallet and install the APK on your Android phone. Head to the [Releases page](https://github.com/adityapk00/zqwandroid/releases) to download. 

### Run the custom version of zec-qt-wallet
In order to let your Android phone connect to your desktop, you need to run a special version of zec-qt-wallet. You can download it from the [releases page](https://github.com/adityapk00/zqwandroid/releases) and run the binary for your platform. Note that the regular version of zec-qt-wallet doesn't let you connect your Android phone.

* You can run your standard version of `zcashd` or let `zec-qt-wallet` run its embedded zcashd
* Although everything will work fine on Mainnet, since this is beta software, I recommend you run it on the zcash testnet. Add `testnet=1` into your `zcash.conf` to enable the testnet

After your node is synced, go to `Apps -> Connect Mobile App` to view the connection QR Code

### Install the Android APK
You'll need to allow `Install from untrusted sources` on your Android phone to install this APK. This is so that you can install the beta APK directly on your phone. (When this is publicly released, you'll be able to get it from the Google Play Store)

After you download and install the APK, launch the program and scan the QR Code from your desktop to connect the two. 

### When you run into bugs, issues or have feature requests
Please file send me all the feedback you can. You can file issues in the [issues tab](https://github.com/adityapk00/zqwandroid/issues). 

#### Currently known limitations
* Old-style Sprout addresses are not supported
* You can't select which address to _send from_. zec-qt-wallet will try to send Transactions from a Sapling address, given you have enough balance
* You can't generate new z-addrs or t-addrs from the phone. 


