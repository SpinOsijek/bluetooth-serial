import type { PermissionState, PluginListenerHandle } from "@capacitor/core";

export interface BluetoothSerialPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  connect(options: { address: string }): Promise<void>;
  disconnect(): Promise<void>;
  read(): Promise<{ data: string }>;
  write(options: { data: string }): Promise<void>;
  available(): Promise<boolean>;
  isEnabled(): Promise<boolean>;
  isConnected(): Promise<boolean>;
  clear(): Promise<void>;
  enable(): Promise<{ isEnabled: boolean }>;
  settings(): Promise<void>;
  list(): Promise<devices>;
  discoverUnpaired(): Promise<devices>;
  checkPermissions(options: { permissions: permissions[] }): Promise<PermissionStatus[]>;
  requestPermissions(options: { permissions: permissions[] }): Promise<PermissionStatus[]>;
  addListener(event: 'discoverUnpaired', listenerFunc: discoverUnpairedCallback): Promise<PluginListenerHandle> & PluginListenerHandle;
  removeAllListeners(): Promise<void>;
}

export interface BluetoothDevice {
  address: string;
  name?: string;
  class?: string;
}

type permissions = 'location' | 'scan' | 'connect';
type PermissionStatus = { [permission in permissions]: PermissionState };
type devices = { devices: BluetoothDevice[] };
type discoverUnpairedCallback = (event: devices) => any;
