'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var core = require('@capacitor/core');

exports.ConnectionState = void 0;
(function (ConnectionState) {
    ConnectionState[ConnectionState["NONE"] = 0] = "NONE";
    ConnectionState[ConnectionState["CONNECTING"] = 1] = "CONNECTING";
    ConnectionState[ConnectionState["CONNECTED"] = 2] = "CONNECTED";
})(exports.ConnectionState || (exports.ConnectionState = {}));

const BluetoothSerial = core.registerPlugin('BluetoothSerial', {
    web: () => Promise.resolve().then(function () { return web; }).then(m => new m.BluetoothSerialWeb()),
});

/* eslint-disable @typescript-eslint/no-unused-vars */
class BluetoothSerialWeb extends core.WebPlugin {
    connect(_options) {
        throw new Error('Method not implemented.');
    }
    connectInsecure(_options) {
        throw new Error('Method not implemented.');
    }
    disconnect() {
        throw new Error('Method not implemented.');
    }
    read() {
        throw new Error('Method not implemented.');
    }
    write(_options) {
        throw new Error('Method not implemented.');
    }
    available() {
        throw new Error('Method not implemented.');
    }
    isEnabled() {
        throw new Error('Method not implemented.');
    }
    isConnected() {
        throw new Error('Method not implemented.');
    }
    clear() {
        throw new Error('Method not implemented.');
    }
    enable() {
        throw new Error('Method not implemented.');
    }
    settings() {
        throw new Error('Method not implemented.');
    }
    list() {
        throw new Error('Method not implemented.');
    }
    discoverUnpaired() {
        throw new Error('Method not implemented.');
    }
    cancelDiscovery() {
        throw new Error('Method not implemented.');
    }
    checkPermissions() {
        throw new Error('Method not implemented.');
    }
    requestPermissions(_options) {
        throw new Error('Method not implemented.');
    }
    async echo(options) {
        console.log('ECHO', options);
        return options;
    }
}

var web = /*#__PURE__*/Object.freeze({
    __proto__: null,
    BluetoothSerialWeb: BluetoothSerialWeb
});

exports.BluetoothSerial = BluetoothSerial;
//# sourceMappingURL=plugin.cjs.js.map
