/// <reference types="vite/client" />

declare module 'json-bigint' {
  const JSONbig: (options?: { storeAsString?: boolean }) => {
    parse: (text: string) => unknown
    stringify: (value: unknown) => string
  }
  export default JSONbig
}
