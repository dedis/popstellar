import PropTypes from 'prop-types';
import STRINGS from "../../res/strings";

const QrReader = (props: IPropTypes) => {
  const { onScan } = props;
  const { onError } = props;
};

const propTypes = {
  onScan: PropTypes.func,
  onError: PropTypes.func,
};
QrReader.prototype = propTypes;

QrReader.defaultProps = {
  onScan:
}
type IPropTypes = PropTypes.InferProps<typeof propTypes>;
export default QrReader;

const onScan = (data: String) => {
  // but I want to add attendees to the list
  // this is too specific
  toast.show(STRINGS.roll_call_scan_participant, {
    type: 'success',
    placement: 'top',
    duration: 4000,
  });
}

export const fireScan = (data: string) => {
  QrReader.prototype.onScan(data);
};
