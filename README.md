# bluetooth-serial

Use native bluetooth functionality

## Install

```bash
npm install bluetooth-serial
npx cap sync
```

## API

<docgen-index>

* [`echo(...)`](#echo)
* [`connect(...)`](#connect)
* [`disconnect()`](#disconnect)
* [`read()`](#read)
* [`write(...)`](#write)
* [`available()`](#available)
* [`isEnabled()`](#isenabled)
* [`isConnected()`](#isconnected)
* [`clear()`](#clear)
* [`enable()`](#enable)
* [`settings()`](#settings)
* [`list()`](#list)
* [`discoverUnpaired()`](#discoverunpaired)
* [`checkPermissions(...)`](#checkpermissions)
* [`requestPermissions(...)`](#requestpermissions)
* [`addListener('discoverUnpaired', ...)`](#addlistenerdiscoverunpaired)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### connect(...)

```typescript
connect(options: { address: string; }) => Promise<void>
```

| Param         | Type                              |
| ------------- | --------------------------------- |
| **`options`** | <code>{ address: string; }</code> |

--------------------


### disconnect()

```typescript
disconnect() => Promise<void>
```

--------------------


### read()

```typescript
read() => Promise<{ data: string; }>
```

**Returns:** <code>Promise&lt;{ data: string; }&gt;</code>

--------------------


### write(...)

```typescript
write(options: { data: string; }) => Promise<void>
```

| Param         | Type                           |
| ------------- | ------------------------------ |
| **`options`** | <code>{ data: string; }</code> |

--------------------


### available()

```typescript
available() => Promise<boolean>
```

**Returns:** <code>Promise&lt;boolean&gt;</code>

--------------------


### isEnabled()

```typescript
isEnabled() => Promise<boolean>
```

**Returns:** <code>Promise&lt;boolean&gt;</code>

--------------------


### isConnected()

```typescript
isConnected() => Promise<boolean>
```

**Returns:** <code>Promise&lt;boolean&gt;</code>

--------------------


### clear()

```typescript
clear() => Promise<void>
```

--------------------


### enable()

```typescript
enable() => Promise<{ isEnabled: boolean; }>
```

**Returns:** <code>Promise&lt;{ isEnabled: boolean; }&gt;</code>

--------------------


### settings()

```typescript
settings() => Promise<void>
```

--------------------


### list()

```typescript
list() => Promise<devices>
```

**Returns:** <code>Promise&lt;<a href="#devices">devices</a>&gt;</code>

--------------------


### discoverUnpaired()

```typescript
discoverUnpaired() => Promise<devices>
```

**Returns:** <code>Promise&lt;<a href="#devices">devices</a>&gt;</code>

--------------------


### checkPermissions(...)

```typescript
checkPermissions(options: { permissions: permissions[]; }) => Promise<PermissionStatus[]>
```

| Param         | Type                                                                    |
| ------------- | ----------------------------------------------------------------------- |
| **`options`** | <code>{ <a href="#permissions">permissions</a>: permissions[]; }</code> |

**Returns:** <code>Promise&lt;PermissionStatus[]&gt;</code>

--------------------


### requestPermissions(...)

```typescript
requestPermissions(options: { permissions: permissions[]; }) => Promise<PermissionStatus[]>
```

| Param         | Type                                                                    |
| ------------- | ----------------------------------------------------------------------- |
| **`options`** | <code>{ <a href="#permissions">permissions</a>: permissions[]; }</code> |

**Returns:** <code>Promise&lt;PermissionStatus[]&gt;</code>

--------------------


### addListener('discoverUnpaired', ...)

```typescript
addListener(event: 'discoverUnpaired', listenerFunc: discoverUnpairedCallback) => Promise<PluginListenerHandle> & PluginListenerHandle
```

| Param              | Type                                                                          |
| ------------------ | ----------------------------------------------------------------------------- |
| **`event`**        | <code>'discoverUnpaired'</code>                                               |
| **`listenerFunc`** | <code><a href="#discoverunpairedcallback">discoverUnpairedCallback</a></code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

--------------------


### Interfaces


#### BluetoothDevice

| Prop          | Type                |
| ------------- | ------------------- |
| **`address`** | <code>string</code> |
| **`name`**    | <code>string</code> |
| **`class`**   | <code>string</code> |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


### Type Aliases


#### devices

<code>{ <a href="#devices">devices</a>: BluetoothDevice[] }</code>


#### PermissionStatus

<code>{ [permission in permissions]: <a href="#permissionstate">PermissionState</a> }</code>


#### permissions

<code>'location' | 'scan' | 'connect'</code>


#### PermissionState

<code>'prompt' | 'prompt-with-rationale' | 'granted' | 'denied'</code>


#### discoverUnpairedCallback

<code>(event: <a href="#devices">devices</a>): any</code>

</docgen-api>
