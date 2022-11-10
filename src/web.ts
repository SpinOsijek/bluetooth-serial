/* eslint-disable @typescript-eslint/no-unused-vars */
import { WebPlugin } from '@capacitor/core';

import type { BluetoothDevice, BluetoothSerialPlugin, devices, permissions, PermissionStatus } from './definitions';

export class BluetoothSerialWeb
  extends WebPlugin
  implements BluetoothSerialPlugin {
  connect(_options: { address: string; }): Promise<BluetoothDevice> {
    throw new Error('Method not implemented.');
  }
  disconnect(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  read(): Promise<{ data: string; }> {
    throw new Error('Method not implemented.');
  }
  write(_options: { data: string; }): Promise<void> {
    throw new Error('Method not implemented.');
  }
  available(): Promise<boolean> {
    throw new Error('Method not implemented.');
  }
  isEnabled(): Promise<boolean> {
    throw new Error('Method not implemented.');
  }
  isConnected(): Promise<boolean> {
    throw new Error('Method not implemented.');
  }
  clear(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  enable(): Promise<{ isEnabled: boolean; }> {
    throw new Error('Method not implemented.');
  }
  settings(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  list(): Promise<devices> {
    throw new Error('Method not implemented.');
  }
  discoverUnpaired(): Promise<devices> {
    throw new Error('Method not implemented.');
  }
  checkPermissions(_options: { permissions: permissions[]; }): Promise<PermissionStatus[]> {
    throw new Error('Method not implemented.');
  }
  requestPermissions(_options: { permissions: permissions[]; }): Promise<PermissionStatus[]> {
    throw new Error('Method not implemented.');
  }
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
