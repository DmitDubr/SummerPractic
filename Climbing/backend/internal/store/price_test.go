package store_test

import (
	"testing"

	"github.com/dimbass/summerpractic/climbing/backend/internal/api"
	"github.com/dimbass/summerpractic/climbing/backend/internal/store"
)

func TestCalcTotalPrice(t *testing.T) {
	pb := api.PriceBreakdown{
		TrainingPrice:      1200,
		ShoesRentalPrice:   200,
		HarnessRentalPrice: 300,
	}

	tests := []struct {
		name string
		eq   api.EquipmentChoice
		want float64
	}{
		{
			name: "own equipment",
			eq:   api.EquipmentChoice{Mode: "OWN"},
			want: 1200,
		},
		{
			name: "rental shoes only",
			eq:   api.EquipmentChoice{Mode: "RENTAL", RentalShoes: true},
			want: 1400,
		},
		{
			name: "rental shoes and harness",
			eq:   api.EquipmentChoice{Mode: "RENTAL", RentalShoes: true, RentalHarness: true},
			want: 1700,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := store.CalcTotalPrice(pb, tt.eq); got != tt.want {
				t.Fatalf("CalcTotalPrice() = %v, want %v", got, tt.want)
			}
		})
	}
}
