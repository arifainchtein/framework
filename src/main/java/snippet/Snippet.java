package snippet;

public class Snippet {
	sudo kill 200009                # stop the broken instance (it has no board to talk to)
	sleep 5; pgrep -f TeleonomeHypothalamus.jar || ./StartHypothalamusBG.sh
}

