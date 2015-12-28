package indexmappers;
public class IndexToLabelMapperImpDefault  implements IndexToLabelMapper {

	@Override
	public String mapIndexToLabel(int index) {
		return Integer.toString(index);
	}

}
