import { toast } from 'react-toastify';

/**
 * Standardized error message extractor for RFC 7807 Problem Details and Axios errors.
 */
export function extractErrorMessage(error, defaultMessage = 'An unexpected error occurred') {
  if (!error) return defaultMessage;
  if (typeof error === 'string') return error;

  const data = error.response?.data;
  if (data) {
    if (typeof data === 'string') return data;
    if (data.detail) return data.detail;
    if (data.message) return data.message;
    if (data.title) return data.title;
    if (data.errors && typeof data.errors === 'object') {
      const fieldErrors = Object.entries(data.errors)
        .map(([field, msg]) => `${field}: ${msg}`)
        .join(', ');
      if (fieldErrors) return fieldErrors;
    }
  }

  return error.message || defaultMessage;
}

export const notify = {
  success: (message, options = {}) => {
    toast.success(message, {
      position: 'top-right',
      autoClose: 3500,
      hideProgressBar: false,
      closeOnClick: true,
      pauseOnHover: true,
      draggable: true,
      ...options,
    });
  },

  error: (errorOrMessage, defaultFallback = 'An unexpected error occurred', options = {}) => {
    const msg = typeof errorOrMessage === 'string'
      ? errorOrMessage
      : extractErrorMessage(errorOrMessage, defaultFallback);

    toast.error(msg, {
      position: 'top-right',
      autoClose: 5000,
      hideProgressBar: false,
      closeOnClick: true,
      pauseOnHover: true,
      draggable: true,
      ...options,
    });
  },

  warning: (message, options = {}) => {
    toast.warning(message, {
      position: 'top-right',
      autoClose: 4000,
      hideProgressBar: false,
      closeOnClick: true,
      pauseOnHover: true,
      draggable: true,
      ...options,
    });
  },

  info: (message, options = {}) => {
    toast.info(message, {
      position: 'top-right',
      autoClose: 3500,
      hideProgressBar: false,
      closeOnClick: true,
      pauseOnHover: true,
      draggable: true,
      ...options,
    });
  },
};

export default notify;
