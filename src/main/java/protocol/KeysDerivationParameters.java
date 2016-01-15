package protocol;

import java.math.BigInteger;
import java.util.Random;

import math.IntegersUtils;
import math.Polynomial;
import math.PolynomialMod;
import math.PolynomialSharing;
import math.PolynomialSharing.PolynomialSharingFactory;
import math.PolynomialSharing.Share;

public abstract class KeysDerivationParameters {
	
	public static class KeysDerivationPrivateParameters {
		public final int i;
		public final PolynomialSharing DRiSharing;
		public final PolynomialSharing betaiSharing;
		public final PolynomialSharing PhiSharing;
		
		private KeysDerivationPrivateParameters(int i, PolynomialSharing betaiSharing, PolynomialSharing PhiSharing,PolynomialSharing DRiSharing) {
			this.i = i;
			this.betaiSharing = betaiSharing;
			this.DRiSharing = DRiSharing;
			this.PhiSharing = PhiSharing;
		}
		
		public static KeysDerivationPrivateParameters gen(ProtocolParameters protocolParameters, int i,BigInteger N, BigInteger Phii, Random rand) {
			BigInteger KN = N.multiply(BigInteger.valueOf(protocolParameters.K));
			BigInteger K2N = KN.multiply(BigInteger.valueOf(protocolParameters.K));
			
			BigInteger betai = IntegersUtils.pickInRange(BigInteger.ZERO, KN, rand);
			BigInteger delta = IntegersUtils.factorial(BigInteger.valueOf(protocolParameters.n));
			BigInteger Ri = IntegersUtils.pickInRange(BigInteger.ZERO, K2N, rand);
						
			PolynomialSharingFactory polynomialSharingFactory = new PolynomialSharingFactory(protocolParameters.k, rand);
			
			PolynomialSharing betaiSharing = polynomialSharingFactory.shareMod(betai, protocolParameters.t, protocolParameters.Pp);
			PolynomialSharing PhiiSharing = polynomialSharingFactory.shareMod(Phii, protocolParameters.t, protocolParameters.Pp);
			PolynomialSharing DRiSharing = polynomialSharingFactory.share(Ri.multiply(delta), protocolParameters.t);
			
			return new KeysDerivationPrivateParameters(i, betaiSharing, PhiiSharing, DRiSharing);
		}
		
	}
	
	public static class KeysDerivationPublicParameters {
		public final int i;
		public final int j;
		public final Share betaij;
		public final Share DRij;
		public final Share Phiij;
		
		private KeysDerivationPublicParameters(int i, int j, Share betaij, Share Rij, Share Phiij){
			this.i = i;
			this.j = j;
			this.betaij = betaij;
			this.DRij = Rij;
			this.Phiij = Phiij;
		}
		
		public static KeysDerivationPublicParameters genFor(int j, KeysDerivationPrivateParameters keysDerivationPrivateParameters) {
			Share Betaij = keysDerivationPrivateParameters.betaiSharing.getShare(j);
			Share DRij = keysDerivationPrivateParameters.DRiSharing.getShare(j);
			Share Phiij = keysDerivationPrivateParameters.PhiSharing.getShare(j);
			return new KeysDerivationPublicParameters(keysDerivationPrivateParameters.i, j, Betaij, DRij, Phiij);
		}
	}
}
