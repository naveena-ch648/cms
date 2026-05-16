import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function OAuthCallbackPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { setAuthData } = useAuth(); // We will need to add this method to AuthContext

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    
    const error = params.get('error');
    if (error) {
      console.error('OAuth error:', error);
      // Redirect to login with error message
      navigate('/login?error=' + encodeURIComponent(error), { replace: true });
      return;
    }

    const accessToken = params.get('access_token');
    const refreshToken = params.get('refresh_token');
    
    if (accessToken && refreshToken) {
      const id = params.get('user_id') || '';
      const email = params.get('email') || '';
      const firstName = params.get('first_name') || '';
      const lastName = params.get('last_name') || '';
      const organizationId = params.get('org_id') || '';
      const organizationName = params.get('org_name') || '';

      const user = {
        id,
        email,
        firstName,
        lastName,
        status: 'ACTIVE',
        organizationId,
        organizationName,
        organizationRole: 'Viewer' // Or fetch real role later
      };

      setAuthData(accessToken, refreshToken, user as any);
      navigate('/', { replace: true });
    } else {
      navigate('/login?error=invalid_tokens', { replace: true });
    }
  }, [location, navigate, setAuthData]);

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
      <p>Completing login...</p>
    </div>
  );
}
