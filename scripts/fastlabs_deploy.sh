cd 	/bahmni/OpenElis
scripts/vagrant-deploy.sh
sudo cp openelis/dist/openelis.war /opt/bahmni-lab/etc/
sudo service bahmni-lab stop
cd /opt/bahmni-lab	
sudo cp ../etc/openelis.war .
sudo jar xvf openelis.war