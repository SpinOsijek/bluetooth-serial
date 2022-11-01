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
  list(): Promise<any[]>;
  discover(): Promise<any[]>;
  checkPermissions(options: { permissions: string[] }): Promise<any[]>;
  requestPermissions(options: { permissions: string[] }): Promise<any[]>;
}
