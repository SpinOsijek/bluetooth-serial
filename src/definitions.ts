import type { PermissionState, PluginListenerHandle } from "@capacitor/core";

export interface BluetoothSerialPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  connect(options: { address: string }): Promise<BluetoothDevice>;
  disconnect(): Promise<void>;
  read(): Promise<{ data: string }>;
  write(options: { data: string }): Promise<void>;
  available(): Promise<{ available: number }>;
  isEnabled(): Promise<{ isEnabled: boolean }>;
  isConnected(): Promise<{ isConnected: boolean }>;
  clear(): Promise<void>;
  enable(): Promise<{ isEnabled: boolean }>;
  settings(): Promise<void>;
  list(): Promise<devices>;
  discoverUnpaired(): Promise<devices>;
  cancelDiscovery(): Promise<void>;
  checkPermissions(): Promise<PermissionStatus[]>;
  requestPermissions(options: { permissions: permissions[] }): Promise<PermissionStatus[]>;
  addListener(event: 'discoverUnpaired', listenerFunc: (event: devices) => any): Promise<PluginListenerHandle> & PluginListenerHandle;
  addListener(event: 'connectionChange', listenerFunc: (event: { state: ConnectionState }) => any): Promise<PluginListenerHandle> & PluginListenerHandle;
  removeAllListeners(): Promise<void>;
}

export interface BluetoothDevice {
  address: string;
  name?: string;
  class?: string;
}

export type permissions = 'location' | 'scan' | 'connect';
export type PermissionStatus = { [permission in permissions]?: PermissionState };
export type devices = { devices: BluetoothDevice[] };
export enum ConnectionState {
  NONE, CONNECTING, CONNECTED,
}