<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>Email Verification - ServeMenu</title>
    <!--[if mso]>
    <style type="text/css">
        body, table, td {font-family: Arial, Helvetica, sans-serif !important;}
    </style>
    <![endif]-->
</head>
<body style="margin: 0; padding: 0; background-color: #f5f5f5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">
    <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%" style="background-color: #f5f5f5;">
        <tr>
            <td style="padding: 40px 20px;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%" style="max-width: 560px; margin: 0 auto; background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%); border-radius: 20px; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);">
                    
                    <!-- Header -->
                    <tr>
                        <td style="padding: 40px 40px 30px 40px; text-align: center;">
                            <h1 style="margin: 0 0 8px 0; font-size: 32px; font-weight: 700; color: #ffffff; letter-spacing: -0.5px;">ServeMenu</h1>
                            <p style="margin: 0; color: rgba(255, 255, 255, 0.7); font-size: 13px; font-weight: 500; letter-spacing: 0.5px; text-transform: uppercase;">Email Verification</p>
                        </td>
                    </tr>
                    
                    <!-- OTP Code Section -->
                    <tr>
                        <td style="padding: 0 40px 30px 40px;">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%">
                                <tr>
                                    <td style="text-align: center;">
                                        <p style="margin: 0 0 24px 0; color: #ffffff; font-size: 15px; line-height: 1.6;">Enter this code to verify your email and complete registration</p>
                                        
                                        <!-- OTP Box -->
                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%">
                                            <tr>
                                                <td style="background-color: #ffffff; border-radius: 12px; padding: 28px; text-align: center; box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);">
                                                    <div style="font-family: 'Courier New', Consolas, monospace; font-size: 44px; font-weight: 700; letter-spacing: 12px; color: #0f172a; line-height: 1;">${otp}</div>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <p style="margin: 20px 0 0 0; color: #00D9FF; font-size: 14px; font-weight: 500;">⏱️ Expires in ${validityMinutes} minutes</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    
                    <!-- Divider -->
                    <tr>
                        <td style="padding: 0 40px;">
                            <div style="height: 1px; background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.2) 50%, transparent 100%);"></div>
                        </td>
                    </tr>
                    
                    <!-- Footer -->
                    <tr>
                        <td style="padding: 24px 40px 40px 40px; text-align: center;">
                            <p style="margin: 0 0 12px 0; color: rgba(255, 255, 255, 0.8); font-size: 12px; line-height: 1.5;">If you didn't request this code, you can safely ignore this email.</p>
                            <p style="margin: 0; color: rgba(255, 255, 255, 0.6); font-size: 11px;">&copy; 2024 ServeMenu • <a href="mailto:support@servemenu.com" style="color: #00D9FF; text-decoration: none;">Support</a></p>
                        </td>
                    </tr>
                    
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
