import type { PermissionState, PluginListenerHandle } from "@capacitor/core";

export interface BluetoothSerialPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  connect(options: { address: string }): Promise<BluetoothDevice>;
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
  addListener(event: 'connectionChange', listenerFunc: connectionChangeCallback): Promise<PluginListenerHandle> & PluginListenerHandle;
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
export type discoverUnpairedCallback = (event: devices) => any;
export type connectionChangeCallback = (event: { state: ConnectionState }) => any;
export enum ConnectionState {
  NONE, CONNECTING, CONNECTED,
}