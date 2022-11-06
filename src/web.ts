import { WebPlugin } from '@capacitor/core';

import type { BluetoothSerialPlugin, devices, permissions, PermissionStatus } from './definitions';

export class BluetoothSerialWeb
  extends WebPlugin
  implements BluetoothSerialPlugin {
  connect(options: { address: string; }): Promise<void> {
    throw new Error('Method not implemented.');
  }
  disconnect(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  read(): Promise<{ data: string; }> {
    throw new Error('Method not implemented.');
  }
  write(options: { data: string; }): Promise<void> {
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
  checkPermissions(options: { permissions: permissions[]; }): Promise<PermissionStatus[]> {
    throw new Error('Method not implemented.');
  }
  requestPermissions(options: { permissions: permissions[]; }): Promise<PermissionStatus[]> {
    throw new Error('Method not implemented.');
  }
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
