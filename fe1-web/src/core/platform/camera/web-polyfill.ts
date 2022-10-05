import { Platform } from 'react-native';

// https://developer.mozilla.org/en-US/docs/Web/API/PermissionStatus
interface PermissionStatusCustom {
  name: string;
  state: 'granted' | 'denied' | 'prompt';
  stream?: MediaStream;
}

export default () => {
  if (Platform.OS !== 'web' || typeof 'navigator' === undefined) {
    return;
  }

  // in general modifying the prototype is very bad practise but
  // unfortunately it is required in this case
  // see https://github.com/dedis/popstellar/issues/1258
  if (navigator.userAgent.toLowerCase().indexOf('firefox') > -1) {
    // prevents issue documented at https://github.com/expo/expo/issues/13182
    // (expo-camera performs a check on this property first)

    // @ts-ignore
    MediaStream.prototype.getVideoTracks = undefined;
  }

  // provide polyfill fallback for navigator.permissions.query for 'camera'
  if (navigator.permissions && navigator.permissions.query) {
    const { query } = navigator.permissions;

    // @ts-ignore
    navigator.permissions.query = (
      permissionDesc: PermissionDescriptor,
    ): Promise<PermissionStatus | PermissionStatusCustom> => {
      return query(permissionDesc).catch((e) => {
        // fallback to polyfill

        const { name } = permissionDesc;

        // @ts-ignore. This does actually happen with expo-camera
        if (name === 'camera') {
          // at the moment we only provide a polyfill for the camera permission request
          // taken from https://github.com/chromium/permissions.request/blob/6302dbd2fa5cbe7488b2534747ac0e51c4df3822/permissions.request.ts#L268
          // and modified

          return new Promise((resolve, reject) => {
            const { constraints, peerIdentity } = permissionDesc as unknown as {
              constraints?: MediaTrackConstraints;
              peerIdentity?: string | undefined;
            };

            navigator.mediaDevices
              .getUserMedia({ video: constraints || true, peerIdentity })
              .then((stream) => resolve({ name, state: 'granted', stream }))
              .catch((err) => {
                if (err.name === 'PermissionDeniedError') {
                  // https://developer.mozilla.org/en-US/docs/Web/API/PermissionStatus
                  resolve({ name, state: 'denied' });
                }
                reject(err);
              });
          });
        }

        // return the same error, this polyfill does not apply
        return Promise.reject(e);
      });
    };
  }
};
