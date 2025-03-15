#!/bin/bash

# Remove src/.env from Git tracking but keep the local file
git rm --cached src/.env

# Confirm the .gitignore has the entry
if ! grep -q "src/.env" .gitignore; then
  echo "src/.env" >> .gitignore
fi

echo "The src/.env file has been removed from Git tracking."
echo "It will remain in your local filesystem but won't be committed to Git anymore."
echo "To complete this process, commit these changes:"
echo "git add .gitignore"
echo "git commit -m \"Remove src/.env from Git tracking\"" 