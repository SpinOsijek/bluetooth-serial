import { WebPlugin } from '@capacitor/core';
import type { BluetoothSerialPlugin, devices, permissions, PermissionStatus } from './definitions';
export declare class BluetoothSerialWeb extends WebPlugin implements BluetoothSerialPlugin {
    connect(_options: {
        address: string;
    }): Promise<void>;
    connectInsecure(_options: {
        address: string;
    }): Promise<void>;
    disconnect(): Promise<void>;
    read(): Promise<{
        data: string;
    }>;
    write(_options: {
        data: string;
    }): Promise<void>;
    available(): Promise<{
        available: number;
    }>;
    isEnabled(): Promise<{
        isEnabled: boolean;
    }>;
    isConnected(): Promise<{
        isConnected: boolean;
    }>;
    clear(): Promise<void>;
    enable(): Promise<{
        isEnabled: boolean;
    }>;
    settings(): Promise<void>;
    list(): Promise<devices>;
    discoverUnpaired(): Promise<devices>;
    cancelDiscovery(): Promise<void>;
    checkPermissions(): Promise<PermissionStatus[]>;
    requestPermissions(_options: {
        permissions: permissions[];
    }): Promise<PermissionStatus[]>;
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
}
