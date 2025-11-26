#!/bin/bash
echo "Starting Room Ruster test - will run every minute"

# Set the Discord webhook URL (use a test webhook or empty to test locally)
# Uncomment and set your test webhook URL if you want to test Discord integration
# export DISCORD_WEBHOOK_URL="your_test_webhook_url_here"

while true; do
    # Clear screen for better visibility
    clear
    
    # Print current time
    echo "=== $(date) ==="
    echo "Running Room Ruster..."
    
    # Run the application with --print to see the output in terminal
    java -jar target/room-ruster-0.1.0.jar --print
    
    # If you want to test Discord integration, uncomment the following line
    # java -jar target/room-ruster-0.1.0.jar --send
    
    # Wait for 1 minute
    sleep 60
done
