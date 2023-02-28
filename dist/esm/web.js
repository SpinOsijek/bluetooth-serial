/* eslint-disable @typescript-eslint/no-unused-vars */
import { WebPlugin } from '@capacitor/core';
export class BluetoothSerialWeb extends WebPlugin {
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
//# sourceMappingURL=web.js.map