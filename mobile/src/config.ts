import Constants from 'expo-constants';

// When testing on a physical device, set apiUrl in app.json (expo.extra.apiUrl)
// to your machine's LAN IP, e.g. http://192.168.1.10:8080
export const API_URL: string =
  (Constants.expoConfig?.extra?.apiUrl as string | undefined) ?? 'http://localhost:8080';
