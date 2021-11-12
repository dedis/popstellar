import PropTypes from 'prop-types';
import STRINGS from 'res/strings';
import { useToast } from 'react-native-toast-notifications';

const QrReader = (props: IPropTypes) => {
  // const { onScan } = props;
  // const { onError } = props;
  const toast = useToast();
  return (
    toast.show(STRINGS.roll_call_scan_participant, {
      type: 'success',
      placement: 'top',
      duration: 4000,
    })
  );
};

const propTypes = {
  onScan: PropTypes.func,
  onError: PropTypes.func,
};
QrReader.prototype = propTypes;

/*
QrReader.defaultProps = {
  onScan:
} */

type IPropTypes = PropTypes.InferProps<typeof propTypes>;
export default QrReader;

/* export const fireScan = (data: string) => {
  QrReader.prototype.onScan(data);
}; */
